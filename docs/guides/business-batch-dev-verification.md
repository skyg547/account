# Issue #690 개발서버 업무 배치 검증

이 가이드는 합성 데이터 주입 → 실제 Spring Batch 실행 → 실행 이력과 업무 결과 확인 순서를 설명한다. Seed는 화면용 mock 응답이 아니라 현재 PostgreSQL 스키마에 맞춘 작은 SQL 입력이다. 실제 고객 데이터는 사용하지 않는다.

## 전제와 범위

- #690의 최초 기반은 `b7c1c7c1`이며 #653의 API 실행 변경을 포함한 `4fc955d7`에서 구현을 시작했다. 재개 시점에 [PR #689](https://github.com/skyg547/account/pull/689)와 HTTP 어댑터 보안 수정 #695가 이미 병합되어, 현재는 `main@56c07971`을 통합한 브랜치에서 검증한다. 이전 체크포인트의 “#689 미병합” 설명은 당시 상태다.
- [API 환경 가이드](business-external-dev-verification.md)의 13개 API 실행 결과와 이번 배치 검증은 구분한다. 이번 작업은 필요한 Journal ID 조회와 Master Data 포트 보정을 단일 API에 반영하고, 배치 JAR의 계산·실행·산출물을 검증한다.
- 기존 PostgreSQL 15개 DB 중 이번 도구가 선택하는 컨텍스트는 12개다. 11개에 업무 Seed를 넣고, Closing은 자체 입력 행 없이 스키마·기간 잠금 상태만 확인한다. 나머지 DB에 주입했다는 의미가 아니다.
- 기존 `account-network`, Master Data / Journal Ledger API와 패키지별 DB checker 컨테이너가 실행 중이어야 한다. 도구는 DB checker의 `psql`을 재사용한다. 새로운 DB·스키마·권한을 만들지 않는다. Master Data provider의 실제 내부 포트는 **8082**이며 소비자 주소는 `http://minimal-master-data:8082`다. Journal Ledger API 내부 포트는 **8080**이므로 아래 Journal by-id 조회의 8080은 그대로 사용한다.
- Python 3, Podman 또는 Docker, JDK 17 빌드 환경, 기존 Gradle 의존성과 `gradle:8.7-jdk17-alpine`, `eclipse-temurin:17-jre-alpine` 이미지가 필요하다. 다운로드가 필요한 환경은 별도 준비한다.
- 승인된 환경 파일의 **경로만** 전달한다. 아래 예시는 기존 `/home/ho/dev/account/.env.external-dev` 경로다. 파일 내용을 출력하거나 shell에서 `source`하지 않는다. 파일은 안전 로더가 허용하는 private regular file이어야 한다.
- 명령은 이슈 worktree 루트에서 실행한다. 아래 각 단계가 성공한 뒤 다음 단계로 이동한다. 빌드와 배치를 동시에 실행하지 않는다.

접속 입력은 각 컨텍스트의 `<PREFIX>_DB_URL`, `_DB_USER`, `_DB_PASSWORD` 계약을 따른다. Master Data는 기존 `DEV_DB_*` 입력도 지원한다. DB 이름은 `<context>_dev`, 실행 계정은 `<context>_dev_app`으로 제한한다. `expenditure`의 실제 DB 명칭은 `expenditure_resolution_dev`다. 값은 명령 인자·가이드·PR에 넣지 않는다.

## 데이터 소유권과 기대값

기준일 **2090-01-15**, 명시적 ID **6900001**부터의 예약 값, 문자열 **GH690**을 사용한다. 이 날짜와 식별자가 다른 작업에 쓰이지 않는지 확인해야 한다. Seed는 identity sequence를 변경하지 않는다. 이미 같은 키에 다른 값이 있으면 덮어쓰지 않고 검증 실패로 중단한다.

| 패키지 / 디렉터리 | 입력과 용도 |
| --- | --- |
| `accounting/master-data` | 계정과목 19개, KRW/USD, 환율 1,400, 열린 2090년 1월 회계기간, 합성 거래처·부서 |
| `accounting/journal-ledger` | POSTED 전표 2개/상세 4개와 GL·SL 원천, 집계에서 제외할 DRAFT 1개 |
| `accounting/payable`, `receivable`, `expenditure` | 채무 1,100, 채권 2,200, 지출결의 3,000과 예산 예약; 이번 실행에서는 입력 적재를 검증 |
| `accounting/closing` | `seed.sql`은 명시적 no-op; 공개 업무 테이블과 합성 기간 잠금 부재 확인; Flyway 이력 직접 조회 없음 |
| `products/deposit` | 잔액 1,000,000 / 연이율 3% 예금 계좌와 개설 거래 |
| `products/loan` | 원금 12,000,000 / 연이율 6%, 이자 60,000·원금 상환 1,000,000 스케줄 |
| `products/asset-lease` | 취득가 12,000,000 / 60개월 자산, 별도 리스 계약·지급 스케줄 |
| `risk/ecl` | Stage 1 익스포저, PD·LGD·CCF·시나리오와 비활성/미담보 설정의 담보 샘플 |
| `risk/account-mart` | ODS 원장과 GL 각각 1,000,000인 기업대출 원천 |
| `risk/reconciliation` | `GH690-CASH`의 외부 SOURCE 1,000,000과 실제 Journal 조회 비교 설정 |

각 디렉터리의 `seed.sql`은 입력, `verify.sql`은 적재 검증, `result*.sql`은 배치 산출물 검증, `rollback.sql`은 검토 후 실행할 정리 계획이다. 상세 수치와 schema/mock/test 출처는 [Accounting](../../tools/seeds/accounting/README.md), [Products Loan](../../tools/seeds/products/loan/README.md), [Risk ECL](../../tools/seeds/risk/ecl/README.md) 등 각 패키지 README에 있다.

Seed는 DB별 `BEGIN`과 advisory transaction lock 안에서 삽입과 `verify.sql`을 함께 실행한다. 충돌로 삽입을 건너뛰더라도 기존 값이 예상과 다르면 그 DB의 전체 트랜잭션이 롤백된다. **여러 DB에 걸친 원자적 트랜잭션은 아니다.** 후속 DB가 실패하면 앞서 완료한 DB의 입력은 남는다. 문제를 확인한 뒤 같은 명령으로 재검증하며, 통과를 위해 기존 값을 강제 수정하지 않는다.

## 1. 8개 실행 JAR 순차 빌드

빌드는 배치 실행과 별개로 CPU 1 / RAM 1536 MiB의 JDK 17 컨테이너를 사용한다. 아래는 저장소 루트에서 실행하는 완전한 명령이다. 현재 사용자의 기존 Gradle 캐시를 쓰며 `--offline`이므로 의존성이 없으면 실패한다. 중간 실패 시 `set -e`가 다음 프로젝트 빌드를 막는다.

```bash
podman run --rm --cpus 1 --memory 1536m \
  --userns keep-id --user "$(id -u):$(id -g)" \
  -v "$PWD:/workspace:rw" \
  -v "$HOME/.gradle:/gradle-cache:rw" \
  -e GRADLE_USER_HOME=/gradle-cache \
  -w /workspace docker.io/library/gradle:8.7-jdk17-alpine \
  sh -ec '
    for project in \
      :journal-ledger:batch \
      :closing:batch \
      :deposit:batch \
      :loan:batch \
      :asset-lease:batch \
      :ecl:ecl-batch \
      :account-mart:mart-batch \
      :reconciliation:batch
    do
      ./gradlew "${project}:bootJar" --offline --no-daemon --max-workers=1 \
        --console=plain \
        "-Dorg.gradle.jvmargs=-Xms128m -Xmx768m -XX:MaxMetaspaceSize=384m -XX:ActiveProcessorCount=1"
    done
  '
```

각 프로젝트의 `build/libs`에 `*-plain.jar`를 제외한 JAR이 정확히 하나 있어야 한다. 빌드 통과는 업무 테스트·실기동 통과와 다르다. 최종 증거에는 대상 테스트 결과와 실제 사용한 JAR 식별자를 함께 기록한다.

## Journal Ledger API 갱신

새 전표 ID 조회 계약은 기존 #653 API 이미지에 없을 수 있다. 수정된 Loan/Closing Batch JAR만 빌드해서는 충분하지 않다. Journal API 테스트와 `:journal-ledger:api:bootJar`를 JDK 17 / 한 worker로 먼저 검증하고 해당 API 하나만 갱신한다. 아래 명령은 절차이며 실제 배포 완료 기록은 최종 증거표에 남긴다.

일반 실행기는 다음과 같이 서비스 범위를 제한한다. `build`는 해당 API 이미지 빌드, `up`은 DB 전제 확인 후 해당 API 기동과 health/Eureka 검증을 수행한다.

```bash
python3 tools/run-business-external-dev.py build --package accounting \
  --service journal-ledger-api --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py up --package accounting \
  --service journal-ledger-api --env-file /home/ho/dev/account/.env.external-dev
python3 tools/run-business-external-dev.py verify --package accounting \
  --service journal-ledger-api
```

이미 테스트·빌드를 완료한 JAR을 재사용할 때는 `tools/Containerfile.prebuilt-java`로 runtime 이미지만 패키징할 수 있다. 다음은 **이미지 생성까지만** 수행하며 서비스 이미지를 교체하지 않는다. 임시 build context에는 `app.jar` 하나만 넣고 환경 파일이나 저장소 전체를 보내지 않는다. 이 runtime 이미지 패키징은 CPU quota 0.5 / RAM 768 MiB, 기존 base image만 사용하는 `--pull=never`, 네트워크 없는 빌드로 수행한다. Podman `build`에서는 `--cpus` 대신 period/quota 옵션을 사용한다. JDK/Gradle 컴파일의 CPU 1 / RAM 1536 MiB 제한과 별개다.

```bash
python3 - <<'PYBUILD'
from pathlib import Path
import shutil
import subprocess
import tempfile

root = Path.cwd()
jars = [p for p in (root / 'journal-ledger/api/build/libs').glob('*.jar')
        if not p.name.endswith('-plain.jar')]
if len(jars) != 1:
    raise SystemExit('Build and test exactly one Journal API bootJar first')
with tempfile.TemporaryDirectory(prefix='account-690-journal-image-') as context:
    shutil.copyfile(jars[0], Path(context) / 'app.jar')
    subprocess.run(['podman', 'build', '--cpu-period', '100000', '--cpu-quota', '50000',
                    '--memory', '768m', '--pull=never', '--network', 'none',
                    '-f', str(root / 'tools/Containerfile.prebuilt-java'),
                    '-t', 'localhost/account-690-journal-api:verified', context], check=True)
PYBUILD
```

이미지 태그의 `verified`는 이름이며 테스트 증거를 대체하지 않는다. 기존 서비스의 CPU 0.5 / RAM 768 MiB 제한을 유지하며 Journal API만 순차 갱신한다. 다른 12개 API나 플랫폼을 일괄 재기동하지 않는다.

아래는 실제 갱신에 사용한 안전 래퍼의 재현 예다. 저장소 루트와 현재 UID를 사용하며, 승인 환경 파일의 identity를 비교하고 기존 실행기의 비출력 명령 처리·메모리 gate·표준 runtime 검증을 재사용한다. 일반 `up`의 300초 한도는 교체 전 timeout이 발생하여 이 경로는 기존 current-user Podman socket과 900초 교체 한도를 사용했다. `config --quiet`만 실행하고 환경 값, 원본 로그, 조회 응답 본문은 출력하지 않는다.

```bash
python3 - <<'PYREFRESH'
import importlib.util
import json
import os
from pathlib import Path
import sys

root = Path.cwd()
spec = importlib.util.spec_from_file_location(
    'runtime', root / 'tools/run-business-external-dev.py')
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
original_run = runtime.run
socket = f'unix:///run/user/{os.getuid()}/podman/podman.sock'

def remote(command, **kwargs):
    if command[0] == 'podman':
        command = ['podman', '--remote', '--url', socket, *command[1:]]
    return original_run(command, **kwargs)

runtime.run = remote
try:
    env_file = Path('/home/ho/dev/account/.env.external-dev')
    before = runtime.identity(env_file)
    runtime.memory_gate()
    # 이전 이미지 객체는 삭제하지 않는다. 이미지 prune도 실행하지 않는다.
    runtime.run(['podman', 'tag', 'localhost/account-690-journal-api:verified',
                 'docker.io/library/account-accounting-external-dev-journal-ledger-api:latest'])
    compose = ['podman', 'compose', '--project-name',
               'account-accounting-external-dev', '--env-file', str(env_file),
               '-f', str(root / 'tools/compose.accounting-external-dev.yml'),
               '--profile', 'external-dev']
    runtime.run([*compose, 'config', '--quiet'])
    if runtime.identity(env_file) != before:
        raise runtime.GateError('input identity changed')
    runtime.run([*compose, 'up', '-d', '--no-build', '--pull', 'never',
                 '--no-deps', '--force-recreate', 'journal-ledger-api'], timeout=900)
    runtime.verify('podman', 'accounting', 'journal-ledger-api', 600)
    container = runtime.container('podman', 'accounting', 'journal-ledger-api')
    result = runtime.run(['podman', 'exec', container, 'wget', '-q', '-T', '5',
                          '-O', '-', 'http://127.0.0.1:8080/api/journals/by-id/6900001'])
    payload = json.loads(result.stdout)
    if payload.get('id') != 6900001 or payload.get('status') != 'POSTED':
        raise runtime.GateError('Journal synthetic by-id assertion failed')
    if runtime.identity(env_file) != before:
        raise runtime.GateError('input identity changed')
    print('PASS Journal refresh, runtime gates, synthetic by-id and input identity')
except (runtime.GateError, ValueError, KeyError):
    print('FAIL Journal refresh gate; inspect safely; original image retained', file=sys.stderr)
    sys.exit(1)
PYREFRESH
```

이 조회 smoke에는 Journal Seed `6900001`이 이미 적재되어 있어야 한다. 최초 환경에서는 다음 Seed 단계로 해당 입력을 먼저 준비한 뒤 갱신 검증을 수행한다. 위 명령은 원본 이미지 객체를 삭제하지 않지만 기존 서비스 태그를 새 이미지로 옮긴다. 교체 전 이미지 ID를 승인된 실행 기록에 보존하고 실패 시 그 정확한 이미지로 해당 서비스만 복구한다. 컨테이너·네트워크·볼륨 전체 삭제나 image prune은 수행하지 않는다.

이전 체크포인트 Journal API JAR의 SHA-256 접두어 `40be7e979ce5`와 실제 배포 이미지의 SHA 라벨 일치를 확인했다. 해당 배포의 health/Eureka, CPU/RAM·restart/OOM gate, 로그 오류 패턴 0, `by-id/6900001`의 POSTED 확인은 PASS다. 이는 해당 시점 Journal API 배포 결과이며 나머지 업무 배치의 완료를 의미하지 않는다. Master Data 연결 포트 8082 보정 후 단일 서비스 갱신과 같은 runtime gate 및 by-id 조회도 통과했다. 이후 테스트를 거친 최신 main 통합 JAR `65eb68586b08cb7d`도 실제 컨테이너 JAR bytes와 이미지 SHA 라벨의 일치를 확인했다. 해당 배포 증거는 아래 최종 표에 기록한다.

## 2. Seed 주입과 적재 확인

```bash
python3 tools/seed-external-dev.py seed --package all \
  --env-file /home/ho/dev/account/.env.external-dev
python3 tools/seed-external-dev.py verify --package all \
  --env-file /home/ho/dev/account/.env.external-dev
```

`all`은 Accounting → Products → Risk 순서이며 Accounting 안에서 Master Data를 먼저 처리한다. `--package accounting`, `products`, `risk`로 범위를 선택할 수 있다. 각 컨텍스트마다 `PASS seed <context>`가 나오고 프로세스가 0으로 종료되어야 한다. 최초 실행과 같은 Seed 명령을 다시 실행해 기존 입력이 보존되는지도 확인한다. 배치로 상태가 바뀐 뒤에는 무조건 재주입하지 말고 해당 입력·결과 검증의 수용 범위를 먼저 확인한다.

기본 엔진은 Podman이다. Docker 환경은 `--engine docker`를 추가한다. 기존 current-user Podman socket을 사용할 때만 다음 옵션을 두 Python 도구에 동일하게 추가한다. 도구가 socket을 생성하거나 서비스를 시작하지는 않는다.

```bash
python3 tools/seed-external-dev.py verify --package all \
  --env-file /home/ho/dev/account/.env.external-dev \
  --podman-socket "/run/user/$(id -u)/podman/podman.sock"
```

## 3. 배치 10개 순차 실행

```bash
python3 tools/run-batch-external-dev.py --job all \
  --env-file /home/ho/dev/account/.env.external-dev --timeout 900
```

`all`의 실제 순서는 아래 표와 같다. Loan은 이자 발생 후 상환하며, Closing provision은 ECL summary가 만들어진 뒤 실행한다. CLI가 식별 파라미터를 고정하므로 별도 날짜·timestamp를 추가하지 않는다. `valuationBatchId`와 `provisionBatchId`는 스크립트가 `6900001,java.lang.Long`으로 전달해 Spring Batch의 Long 타입 계약을 지킨다.

| 순서 / `--job` | 실제 Job / 식별 파라미터 | 입력 → 처리 → 확인할 출력 |
| --- | --- | --- |
| 1 `balances` | `dailyBalanceReaggregationJob`; `startDate`, `endDate` = 기준일 | POSTED 상세 → GL/SL 재집계 → 각각 4개 잔액 키, KRW ±1,000,000와 USD의 기준통화 환산액 ±130,000. DRAFT 999 제외 |
| 2 `fx` | `fxValuationJob`; `valuationDate` = 기준일, `valuationBatchId=6900001` | 외화 자산·부채 100 USD / 장부액 각각 130,000 → 환율 1,400 평가 → 이익 10,000·손실 10,000의 균형 DRAFT 전표 2개 |
| 3 `deposit` | `depositAccountIntegrityJob`; `asOfDate` = 기준일, `run.id=6900001` | 활성 계좌 → 잔액·이율·기간 무결성 검사 → 입력 보존과 정확한 Job 완료 기록; 별도 업무 결과 테이블은 만들지 않음 |
| 4 `loan-interest` | `loanInterestAccrualJob`; `accrualDate` = 기준일 | EIR 스케줄 → 이자 60,000 발생 → 성공 accrual log와 실제 전표 연결 |
| 5 `loan-repayment` | `loanScheduledRepaymentJob`; `repaymentDate` = 기준일 | 성공 이자 발생·스케줄 → 원금 1,000,000 + 이자 60,000 상환 → 현재 원금 11,000,000, 누적 상환액, 성공 이벤트와 전표 연결, pending 없음 |
| 6 `depreciation` | `assetDepreciationJob`; `targetDate` = 기준일 | 고정자산 → 월 감가상각 200,000 → 누적상각 200,000 / 장부가 11,800,000. 리스 지급 처리나 전표 생성은 이 잡의 검증 대상이 아님 |
| 7 `ecl` | `allowanceEclJob`; `baseDate` = 기준일, `runId=GH690`, `modelVersion=GH690` | 익스포저·모델 → EAD 1,100,000, Stage 1, LGD .45 → ECL 4808.5714와 Closing용 summary |
| 8 `mart` | `integratedPositionEtlJob`; `baseDate` = 기준일, `run.id=6900001` | ODS·GL → 변환/대사 → Stage 1 position/exposure snapshot, GL–SL 차이 0 |
| 9 `reconciliation` | `reconciliationDailyJob`; `reconciliationDate` = 기준일, `runBy=GH690`, `deepMode=false`, `run.id=6900001` | SOURCE와 실제 Journal 조회 → 얕은 대사 → 업무 상태 SUCCESS, 일치 1건, 차이 0 |
| 10 `provision` | `eclProvisionJob`; `closingDate` = 기준일, `provisionBatchId=6900001` | ECL summary → 충당금 차변/대변 결정 → `GH690-BADDEBT` / `GH690-ALLOWANCE` DRAFT 전표 1개 |

ECL 기대값은 `1,100,000 × .45 × .01 × (.8×.2 + 1×.6 + 1.3×.2) / 1.05 = 4808.5714`다. Journal의 `NUMERIC(19,2)` 저장 결과 **4808.57**과 구분해 검증한다. Closing 자동 전기는 꺼져 있으므로 FX/ECL 결과는 DRAFT다. Mart의 downstream 이벤트 발행도 꺼져 있고 ECL에는 독립 입력 fixture가 있다. 따라서 Mart → ECL 자동 전달이나 deep 대사까지 통과했다고 해석하지 않는다.

Deposit·Mart·Reconciliation에는 기존 `RunIdIncrementer`가 있어 Boot가 실행 ID를 추가한다. 이 도구는 세 잡에 `run.id=6900001,java.lang.Long`을 명시해 자동 증가로 다른 JobInstance가 생성되지 않도록 한다. 다른 잡에는 불필요한 `run.id`를 추가하지 않는다.

완료 캐시 조회는 식별 파라미터의 이름·값·Java 타입·identifying 플래그와 개수가 정확히 같아야 한다. 값이 같아도 Long 대신 String이거나 추가 `run.id`가 있으면 다른 실행이다. Loan은 로컬 로그/이벤트에 저장된 전표 ID·번호가 원격의 해당 합성 POSTED 전표와 동일한지도 대조한다.

ECL 일반 실행의 기본 executor는 core 4 / max 8, grid 4를 유지한다(test 프로파일은 worker 1). 이 도구가 명시적으로 `account.ecl.batch.worker-threads=1`, `account.ecl.batch.grid-size=1`을 적용한다. 이 설정은 JobParameters가 아니며 재시작 중 grid를 변경하지 않는다.

저자원 환경의 첫 금융 요청 지연을 고려해 이 도구는 Closing·Loan·Deposit·Reconciliation HTTP 연결 10초 / 응답 60초를 명시한다. 업무별 기본 설정을 바꾸거나 쓰기를 자동 재시도하지 않는다. 실제 FX 실패 중 전표 한 건이 이미 저장된 사례는 금융 내용과 이력을 먼저 검증한 뒤 같은 식별자로 재시작했다. 프로세스 실패를 원격 쓰기 미발생으로 해석하면 안 된다.

실행기는 매번 CPU **0.5** / RAM **768 MiB**, JVM heap 최대 448 MiB, metaspace 최대 160 MiB로 배치 컨테이너 하나만 실행한다. Seed와 Batch는 동일 사용자/worktree 간 공통 잠금을 사용하고 기존 이슈 배치 실행 또는 unresolved marker가 있으면 중단한다. `--timeout`은 개별 잡의 실행 대기 한도다. 범용 동시 실행 도구나 shell `&`로 병렬화하지 않는다.

DB 연결 풀은 최소 유휴0, 최대2다. Loan은 JPA paging reader·외부 chunk·건별 `REQUIRES_NEW` 트랜잭션이 연결을 동시에 보유하므로 최대3을 사용한다. 최초 이자 실행은 최대2에서 연결 획득 15초 timeout으로 실패했고, 로컬 이자 기록과 원격 합성 전표가 모두 없는 것을 확인한 뒤 동일 파라미터로 재시작했다. 업무 트랜잭션 경계와 다른 모듈의 풀은 유지한다. 후속 시도에서는 이자수익 계정 설정 누락으로 FAILED 이력1건이 남고 원격 전표는 생성되지 않았다. 실행기에 시드·결과 SQL과 일치하는 `interest-income-account-code=410100`을 명시해 같은 잡 인스턴스로 재시작한다.

Spring `dev`로 실행하되 웹 서버·Discovery·Config/Vault·Kafka listener는 비활성화한다. DB 스키마는 validate만 하고 Flyway·SQL 초기화·Batch schema 생성은 비활성화한다. 금융 계산에 필요한 Master Data / Journal 호출은 실제 개발 API를 사용한다. 기존 데이터 중 잡의 날짜/전체 활성 데이터 조회 범위에 무관한 행이 걸리면 scope gate가 실행을 거부한다. Mart는 ODS 원천뿐 아니라 기준일의 `allowance_input_positions`와 `allowance_exposure_snapshots` 두 결과 테이블도 검사해 무관한 결과가 교체되지 않도록 한다. 이를 통과시키기 위해 조회 범위를 넓히거나 기존 행을 지우지 않는다.

현재 후속 검증에서는 Deposit의 local adapter 기본값 `true`가 dev에도 적용되어 HTTP adapter와 중복되고 컨텍스트 시작이 실패한 점을 보정했다. `application-dev.yml`에 `account.deposit.local-adapters.enabled=false`를 명시하고 실제 배치 애플리케이션 컨텍스트에서 HTTP 포트만 선택되는지 확인하는 회귀 테스트를 추가했다. Deposit 테스트4개와 수정 JAR 실행1이 모두 통과했다. Mart는 custom runner와 Boot runner의 이중 실행 위험을 보정하여 Boot가 켜지면 custom runner를 제외한다. 테스트8개, 실제 실행1과 산출물을 통과했고 해당 기준일의 실제 실행이 정확히1건임을 추가 확인했다.

## 4. 완료와 재실행 판정

신규 실행의 성공 조건은 **실제 컨테이너 inspect의 CPU 0.5 / RAM 768 MiB 상한**, **컨테이너 exit 0 / OOM false**, 해당 Job과 식별 파라미터의 **새 `BATCH_JOB_EXECUTION` status·exit_code 모두 COMPLETED**, **`result*.sql`이 단일 `true`**인 것이다. 단순 프로세스 종료나 빈 입력으로 끝난 Job만으로 성공을 선언하지 않는다. Loan은 신규 실행과 완료 캐시 검증 모두 Journal DB의 `result-loan-interest.sql` / `result-loan-repayment.sql`도 자동 검사한다. 이자 전표는 60,000 차변/대변 2개 라인, 상환 전표는 현금 차변 1,060,000과 원금 1,000,000·미수이자 60,000 대변의 3개 라인, POSTED 상태와 lineage를 확인한다. 상환 현금 계정은 `GH690-REPAYCASH`로 분리하여 대사 대상 `GH690-CASH`의 1,000,000 잔액에 섞이지 않게 한다.

```bash
# 같은 식별 파라미터로 전체 완료 결과를 다시 확인한다.
python3 tools/run-batch-external-dev.py --job all \
  --env-file /home/ho/dev/account/.env.external-dev

# 실패 원인을 해결한 뒤 특정 잡만 동일 조건으로 다시 확인한다.
python3 tools/run-batch-external-dev.py --job loan-repayment \
  --env-file /home/ho/dev/account/.env.external-dev
```

이미 COMPLETED이면 새 컨테이너/JobInstance를 만들지 않고 기존 완료 이력과 업무 결과를 다시 검사한다. 이때의 `PASS ... existing completed instance`는 **재실행 캐시 검증**이며 새 계산 실행의 증거가 아니다. FAILED/STOPPED는 원인과 업무 상태를 확인한 뒤 같은 파라미터로 재시도한다. STARTING/STARTED/STOPPING/UNKNOWN은 자동 재시작하지 않는다. Batch metadata를 삭제하거나 날짜를 바꿔 중복 실행 방어를 우회하지 않는다.

## 5. 실패 확인과 복구

도구는 자격증명이 포함될 수 있는 하위 프로세스 출력과 원본 로그를 숨긴다. 아래처럼 이름·상태·라벨·종료 상태만 확인한다. `Config.Env`, 전체 inspect JSON, 환경 파일, 원본 로그를 게시하지 않는다.

```bash
podman ps -a --filter label=account.issue=690 \
  --format '{{.Names}} {{.Status}}'
```

정확한 실패 컨테이너 이름을 위 결과에서 선택한 뒤 다음 명령을 사용한다. 예시의 값을 실제 확인한 이름으로 바꾼다.

```bash
batch_container_name=account-690-loan-repayment-REPLACE_WITH_ACTUAL_SUFFIX
podman inspect --format '{{index .Config.Labels "account.issue"}} {{.State.Running}} {{.State.ExitCode}} {{.State.OOMKilled}}' "$batch_container_name"
# 이슈 690 소유임을 확인한 컨테이너만 중지한다.
podman stop --time 10 "$batch_container_name"
podman inspect --format '{{.State.Running}}' "$batch_container_name"
```

프로세스 강제 종료나 엔진 응답 유실 시 `/tmp/account-business-batch-<UID>.pending`이 남을 수 있다. 다음 명령은 공통 실행 잠금 안에서 기록된 이름 형식과 `account.issue=690` 소유 라벨을 확인하고 **그 컨테이너만** 복구 대상으로 삼는다. 이미 `Running=false`이면 stop을 생략하고, `true`일 때만 중지한 뒤 다시 `false`인지 확인하여 marker를 해제한다. 실행 요청이 컨테이너 생성 전에 실패한 경우에는 `ps --all` 명령 성공과 정확한 기록 이름의 부재를 확인한 때만 marker를 해제한다. 새 잡을 실행하거나 Batch metadata를 수정하지 않는다.

```bash
python3 tools/run-batch-external-dev.py --recover \
  --env-file /home/ho/dev/account/.env.external-dev
```

기존 current-user socket으로 실행했다면 복구에도 같은 `--podman-socket` 옵션을 사용한다. marker 부재, 이름/소유권 불일치, 엔진 응답 실패 또는 정지 확인 실패는 복구 실패로 처리한다. marker를 임의 삭제하여 우회하지 않는다. 복구 성공 뒤에도 Job metadata와 원격 상환 예약 상태를 확인해야 한다. 컨테이너 중지는 업무 트랜잭션 복구를 의미하지 않는다. 최신 복구 경로의 실제 실행은 PASS다. 엔진 명령과 컨테이너 launch의 한도는 300초이며 잡 자체의 `--timeout`과 구분한다. 실패 후 정리 과정에서 추가 timeout이 나더라도 실행기는 원래의 비밀정보를 제외한 실패 원인을 보존한다.

Loan의 `SCHEDULED_REPAYMENT_PENDING`은 컨테이너 marker와 다르다. Core가 별도 트랜잭션에 상환 예약을 저장하고 원격 전표를 요청한 뒤 원금·완료 이벤트를 저장한다. 네트워크 timeout이나 중간 종료는 원격 전기 성공 여부가 불명확할 수 있어 pending을 보존한다. 담당자가 `LOAN_SCHEDULED_REPAYMENT` lineage와 합성 loan/date의 원격 전표, 로컬 원금·이벤트를 대조하고 완료 또는 취소 복구를 결정해야 한다. 자동 보상/복구 명령은 제공하지 않는다. pending 행 삭제나 같은 전표 재발행으로 재시도를 강제하지 않는다.

### FX 실행 실패와 재시작 조건

FX 첫 `execution=1`은 `ReaderNotOpenException`으로 FAILED이며 원격 FX 전표 생성은 0건이었다. `@StepScope` reader 메서드의 반환형을 `JdbcCursorItemReader`로 보정하여 Spring Batch의 stream open/update/close 수명주기가 scoped proxy를 통해 유지되도록 했다. 실제 partitioned 실행의 수명주기를 확인하는 회귀 검증도 추가했다. 이 책임은 Batch reader 구성에 있고 금융 평가 산식은 Core에 유지한다.

FX `execution=2`는 reader 단계에 정상 진입했으나 Journal DRAFT 생성 요청에서 HTTP 400으로 실패했다. 후속 진단에서 Master Data provider는 8082로 수신하는데 소비자 설정은 8080을 가리키는 불일치를 확인했다. Accounting/Products Compose, Journal dev fallback, Batch 실행기의 Master Data 주소를 8082로 보정하고 Journal 단일 서비스 갱신과 연결 검증을 완료했다.

연결 검증은 변경한 소비자에서 동일 `account-network`의 `minimal-master-data:8082`에 접근하는지 확인하고, 합성 기준일·계정과목의 실제 참조 조회를 응답 본문 비출력 방식으로 검증한다. health 성공만으로 Journal DRAFT 생성 검증을 대신하지 않는다. Journal 설정이 이미지에 포함되면 수정 JAR을 재패키징하고 앞 절차로 해당 서비스만 갱신한다. 승인된 rollback용 원본 Journal 이미지를 계속 보존한다.

FX `execution=3`은 HTTP 상태가 없는 원격 호출 실패로 종료되었지만, 금액 10,000의 차대변이 균형인 DRAFT 전표 1건이 이미 저장되어 있었다. 원격 쓰기가 없었다고 가정하지 않고 전표 내용과 이력을 먼저 확인했다. 개발 검증용 HTTP 연결 10초 / 응답 60초 상한을 적용한 뒤 **같은 식별 파라미터**로 재시작한 `execution=4`는 `COMPLETED`, 컨테이너 exit 0 / OOM false였고 기대한 FX DRAFT 전표 2건을 확인했다. FAILED metadata를 삭제하거나 새로운 batch ID로 우회하지 않았다. 이 결과는 FX의 확인 결과이며 나머지 배치의 최종 성공을 뜻하지 않는다.

### 대사 원천 조회 보정

최초 대사 시도는 개발 HTTP 어댑터의 상세·집계 조회가 빈 목록·0을 반환해 실제 입력과 불일치했고, 차이 사유 코드 검사에서 실패했다. 실패 후 부분 차이 행이 없음을 확인했다. 기대 결과나 차이 사유 시드를 바꾸지 않고, 기존 Journal 기간 API의 실제 전표 라인을 읽도록 수정했다.

조회는 `POSTED`, 회계일자, 계정, 집계 차대 구분을 적용하고 `BigDecimal`로 기준금액(없으면 거래금액)을 합산한다. HTTP 오류·리다이렉트·불완전 데이터는 실패로 처리한다. 전표마다 추가 요청하지 않지만 기간 전체 응답을 메모리에 읽고, 집계와 상세 요청 사이의 분산 스냅샷은 보장하지 않는다. 작은 고정 합성 데이터로 검증하며 대량 기간 조회 성능 인증은 별도다. 자세한 흐름은 [대사 프로세스](../../reconciliation/docs/process-flow.md)를 참고한다.

## 6. 데이터 정리와 책임 경계

`rollback.sql`은 자동 실행되지 않는다. 업무 결과와 전표 lineage를 먼저 확인하고, 생성·전기된 FX/ECL/Loan 전표는 별도 역분개/정리 판단 후 Seed를 정리한다. 파일별 SQL을 안전 입력 도구와 같은 자격증명 비출력 경로에서 **트랜잭션으로** 실행해야 한다. 현재 `seed-external-dev.py`에는 `rollback` 옵션이 없다.

정리는 Risk / Products의 종속 결과 → Accounting 종속 데이터 → Journal → Master Data 순으로 FK와 외부 참조를 확인한다. Closing이 ECL summary를 사용하는 동안 ECL을 먼저 지우지 않는다. 각 파일은 소유 ID/업무키 중심의 child-before-parent 삭제이며 Batch 실행 이력·identity sequence·다른 사용자 데이터는 보존한다. 후속 지급/수금 등 새 참조가 생겼다면 FK 실패를 우회하지 않는다. DB 간 정리도 원자적이지 않으며 Seed 롤백만으로 원격 전표까지 되돌아가지 않는다.

Payable·Receivable·Expenditure·Deposit·Loan·Asset Lease의 정리 SQL은 삭제 전에 소유권과 후속 업무 참조를 검사한다. 하나의 PostgreSQL `DO` 블록으로 실행하므로 검사가 실패하면 상세 일부만 삭제되는 결과를 만들지 않는다. 관련 테이블 잠금이 업무 쓰기를 막으므로 서비스 쓰기가 중지된 점검 시간에만 실행하고, `statement_timeout`·`lock_timeout`을 설정한다. Loan에 이자/상환 이력이 있거나 다른 패키지에 전표·지급·수금 참조가 있으면 먼저 정리하지 않고 거부한다. 성공한 배치 전표를 자동으로 역분개하지 않는다.

회귀 테스트는 `python3 -m unittest discover -s tools/seeds/tests -p 'test_*.py'`로 실행한다. SQLite 검사는 소유권 조건과 삭제 거부를 검증하며 PostgreSQL 문법·잠금 증거는 별도의 실제 트랜잭션 검증 결과를 따른다.

Batch 설정은 Job/Step 순서, 청크, 분할과 식별 파라미터를 담당한다. 재무 공식과 상태 전이는 다음 Core 구현에서 확인한다.

| 관심사 | Batch → Core 책임 |
| --- | --- |
| 잔액/외화평가/충당금 | `BalanceReaggregationBatchConfig` → `LedgerService`; `FxValuationBatchConfig` → `FxValuationPipeline` / `FxValuationService`; `EclProvisionBatchConfig` → `EclProvisionService` |
| 예금/대출 | `DepositAccountIntegrityBatchConfig` → `DepositBatchUseCase`; Loan config → 이자 발생 service / `ScheduledRepaymentService`와 Loan 도메인 |
| 자산 | `AssetDepreciationBatchConfig` → `DepreciationPipeline`; 영속성은 Core outbound adapter |
| 리스크 | `ecl-batch`, `mart-batch`, `reconciliation/batch`는 실행 조정; 모델·변환·대사 규칙과 bulk 쓰기는 각 Core의 pipeline/service/adapter |

스크립트의 안전 실행과 검증 책임은 `tools/business_batch_support.py`, `seed-external-dev.py`, `run-batch-external-dev.py`에 있다. Seed SQL에 업무 계산 구현을 대체하는 결과 행을 넣지 않는다. 의미 있는 로직 주석에는 입력→처리→출력, 트랜잭션 경계, 재실행/원격 불확실성 처리 이유를 남긴다.

## 검증 증거 — 2026-09-11 Integrator 기록

| 검증 | 결과·근거 |
| --- | --- |
| Seed | 11개 업무 DB + Closing readiness1, 총12 컨텍스트 재주입·적재 검증 PASS. 초기2회 이후 최종 코드 재주입도 PASS |
| Python | Batch/seed 실행 도구55, 기존 API 실행 도구38, SQL 정리 회귀10 모두 PASS |
| PostgreSQL 정리 SQL | 6개 DO/잠금/삭제를 실제 실행 후 ROLLBACK; 시드 보존. 전표 연결 지출결의는 삭제 거부·상세 보존 |
| Java | 18개 프로젝트 테스트 총972, failures/errors/skips0. 변경 없는 Asset Batch3건은 유효한 Gradle up-to-date 결과 재사용 |
| Build | JDK17·offline·CPU1/RAM1536MiB·`--max-workers=1`, 8개 batch bootJar 및 영향 API 빌드/컴파일 PASS |
| Journal API | 실제 배포 JAR `65eb68586b08cb7d86ec964ed2cbe6b67205cf5e74181a3f9fc43858945dd28c`; 컨테이너 bytes·이미지 label 일치, health/Eureka UP, restart0/OOMfalse·오류0·by-id POSTED |
| 실기동 | 아래10잡의 STATUS/EXIT_CODE 모두 COMPLETED, 컨테이너 exit0/OOMfalse, 실제 CPU0.5/RAM768MiB. 한 번에1개씩 실행 |
| 동일 파라미터 재검증 | 10/10 기존 완료 이력·결과 재검증 PASS, 신규 컨테이너/잡 실행 없음. Loan 두 DB 전표 ID/번호 일치. Mart 해당 기준일 실행 정확히1건 |
| Compose / 하네스 | Accounting·Products 비출력 render·입력 identity PASS, Node 계약135/135 PASS |

| 잡 | COMPLETED 실행 ID | 검증된 업무 결과 | 실제 실행 JAR SHA256 앞16자리 |
| --- | --- | --- | --- |
| `balances` | 1 | GL/SL 기대 4개 키·잔액 | `d79e9480a37b46c7` |
| `fx` | 4 | FX DRAFT 2건, 이익·손실 각10,000, 차대 균형 | `c3e1bcce72a23812` |
| `deposit` | 1 | 원장·거래 합계1,000,000 유지 | `a9f2044fd9d02c41` |
| `loan-interest` | 3 | 이자60,000, POSTED 전표·DB 간 ID/번호 일치 | `e17564962dc6d75c` |
| `loan-repayment` | 4 | 원금잔액11,000,000, 원금납부1,000,000·이자60,000, pending0 | `e17564962dc6d75c` |
| `depreciation` | 1 | 상각200,000, 누계200,000, 장부가11,800,000 | `ca9a9d166e78969a` |
| `ecl` | 1 | Stage1, EAD1,100,000, LGD0.45, ECL/summary4,808.5714 | `e2d8cf15d7b97d26` |
| `mart` | 1 | 포지션·노출1,000,000, 집계차이0·DQ오류0 | `71c2c6ad0abe4af3` |
| `reconciliation` | 2 | SUCCESS, 원천·대상 각1건/1,000,000, 차이0 | `8c46768ab53a5533` |
| `provision` | 5 | ECL summary 기반 DRAFT4,808.57·차대 균형 | `c3e1bcce72a23812` |

`balances` 실행1은 최초 JAR로 실제 완료한 이력이다. main 통합 후 최신 JAR `aa365b1b87304816`도 빌드·관련 테스트했으나 완료 잡을 다시 실행하지 않았다. 현재 엄격한 파라미터 타입·식별 집합과 결과 SQL로 재검증한 사실을 새 JAR 실기동과 구분한다. 나머지는 위 JAR로 실제 완료했다.

최종 명령은 이 가이드의 `--job all` 실행과 같은 명령의 재실행이다. 신규 실행은 `PASS <job>: execution=N COMPLETED, outputs, exit0/OOMfalse`, 완료 재사용은 `PASS <job>: existing completed instance N, output assertions`로 구분한다. 증거 원본은 Integrator의 `/tmp/gh690-live-batches.log`, `/tmp/gh690-repeat-batches.log`, `/tmp/gh690-verification.json`이며 재현 가능한 명령·결과는 이 문서와 PR에 남긴다. 비밀정보와 원문 앱 로그는 포함하지 않는다.

실제 검증은 rootless Podman이며 Docker 실기동은 주장하지 않는다. ECL은 독립 Stage1 입력, Mart→ECL 자동 이벤트·deep 대사·자동 전표 전기는 제외했다. FX/ECL은 DRAFT, Deposit은 읽기 전용 무결성 검사다. 성공한 Loan 전표는 Seed 정리로 되돌리지 않는다.

| 항목 | 판정 (PASS/FAIL/N/A) | 파일·테스트 근거 | N/A 사유 | 위험·다음 검증 게이트 | 독립 리뷰 확인 |
| --- | --- | --- | --- | --- | --- |
| Q1 | PASS | Core Loan 예약·전표 정합성, Closing source ports, Reconciliation HTTP 조회; Java972·실제10잡·SQL 결과 | 전 항목 적용 | 작은 합성 데이터 검증; 대량·운영 검증 별도 | 독립 XML·해시·실기동 증거 대조 PASS |
| Q2 | PASS | Loan process-flow, 동일 식별 파라미터·완료 재사용, 실패/부분 원격 쓰기 복구 기록 | 전 항목 적용 | pending 수동 대사·복구 | 독립 source review PASS |
| Q3 | PASS | business-batch-dev-verification, Loan/Mart/Reconciliation 모듈 문서와 실제 실행표 | 전 항목 적용 | Draft CI·사람 리뷰 | 독립 문서·실행 결과 대조 PASS |
| Q4 | PASS | scoped reader, 정확한 전표 multiset, 읽기전용 source pool, 원자적 SQL 정리, 실제 HTTP 필터 주석 | 전 항목 적용 | 기간 HTTP 조회의 메모리·분산 스냅샷 한계 문서화 | 독립 source review PASS |

Draft PR [#704](https://github.com/skyg547/account/pull/704)에 `Refs #690`과 검증 결과·잔여 제한을 기록했다. **권한 분리: Reviewer 승인 후 Integrator만 병합. 구현 담당은 병합하지 않음.** Ready·merge·Issue close는 별도 승인 게이트다.

package com.ho.account.receivable.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.receivable.api.dto.CollectionRequest;
import com.ho.account.receivable.api.dto.CollectionResponse;
import com.ho.account.receivable.api.dto.ManualMatchingRequest;
import com.ho.account.receivable.application.port.in.CollectionCommand;
import com.ho.account.receivable.application.port.in.CollectionUseCase;
import com.ho.account.receivable.application.port.in.ManualMatchingCommand;
import com.ho.account.receivable.domain.Collection;
import com.ho.account.receivable.domain.CollectionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class CollectionControllerTest {

    @Test
    void receivePayment_acceptsRequestDtoAndReturnsResponseDto() {
        CapturingCollectionUseCase useCase = new CapturingCollectionUseCase();
        CollectionController controller = new CollectionController(useCase);
        CollectionRequest request = new CollectionRequest();
        request.setCollectionDate(LocalDate.of(2026, 5, 10));
        request.setCustomerCode("CUST-001");
        request.setAmount(new BigDecimal("550.00"));
        request.setBankAccount("BANK-001");
        request.setVirtualAccount("V-001");
        request.setReferenceNo("INV-001");

        ResponseEntity<CollectionResponse> response = controller.receivePayment(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCustomerCode()).isEqualTo("CUST-001");
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("550.00");
        assertThat(response.getBody().getStatus()).isEqualTo(CollectionStatus.RECEIVED);
        assertThat(useCase.receivedCommand.referenceNo()).isEqualTo("INV-001");
    }

    @Test
    void getUnmatchedCollections_returnsResponseDtos() {
        CapturingCollectionUseCase useCase = new CapturingCollectionUseCase();
        Collection unmatched = collection("CUST-002", "250.00");
        unmatched.markAsUnmatched();
        useCase.unmatchedCollections = List.of(unmatched);
        CollectionController controller = new CollectionController(useCase);

        ResponseEntity<List<CollectionResponse>> response = controller.getUnmatchedCollections();

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCustomerCode()).isEqualTo("CUST-002");
        assertThat(response.getBody().get(0).getStatus()).isEqualTo(CollectionStatus.UNMATCHED);
    }

    @Test
    void manualMatchCollection_acceptsDedicatedRequestDto() {
        CapturingCollectionUseCase useCase = new CapturingCollectionUseCase();
        CollectionController controller = new CollectionController(useCase);
        ManualMatchingRequest request = new ManualMatchingRequest();
        request.setCollectionId(10L);
        request.setReceivableId(20L);
        request.setMatchingAmount(new BigDecimal("300.00"));

        controller.manualMatchCollection(request);

        assertThat(useCase.matchedCommand.collectionId()).isEqualTo(10L);
        assertThat(useCase.matchedCommand.receivableId()).isEqualTo(20L);
        assertThat(useCase.matchedCommand.matchingAmount()).isEqualByComparingTo("300.00");
    }

    private static Collection collection(String customerCode, String amount) {
        Collection collection = new Collection();
        collection.setCollectionDate(LocalDate.of(2026, 5, 10));
        collection.setCustomerCode(customerCode);
        collection.setAmount(new BigDecimal(amount));
        collection.setStatus(CollectionStatus.RECEIVED);
        return collection;
    }

    private static class CapturingCollectionUseCase implements CollectionUseCase {

        private CollectionCommand receivedCommand;
        private List<Collection> unmatchedCollections = List.of();
        private ManualMatchingCommand matchedCommand;

        @Override
        public Collection receivePayment(CollectionCommand command) {
            this.receivedCommand = command;
            Collection collection = new Collection();
            collection.setCollectionDate(command.collectionDate());
            collection.setCustomerCode(command.customerCode());
            collection.setAmount(command.amount());
            collection.setBankAccount(command.bankAccount());
            collection.setVirtualAccount(command.virtualAccount());
            collection.setReferenceNo(command.referenceNo());
            collection.setStatus(CollectionStatus.RECEIVED);
            return collection;
        }

        @Override
        public void attemptAutoMatching(Long collectionId) {
        }

        @Override
        public void manualMatchCollection(ManualMatchingCommand command) {
            this.matchedCommand = command;
        }

        @Override
        public List<Collection> getUnmatchedCollections() {
            return unmatchedCollections;
        }
    }
}
package com.ho.account.closing.repository;

import com.ho.account.closing.domain.ClosingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClosingTaskRepository extends JpaRepository<ClosingTask, Long> {
}

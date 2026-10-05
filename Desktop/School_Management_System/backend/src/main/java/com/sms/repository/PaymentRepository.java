package com.sms.repository;

import com.sms.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStudentId(Long studentId);

    List<Payment> findByInvoiceId(Long invoiceId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    @Query(value = "SELECT TO_CHAR(payment_date, 'Mon YYYY') AS label, COALESCE(SUM(amount), 0) " +
            "FROM payments " +
            "GROUP BY TO_CHAR(payment_date, 'YYYY-MM'), TO_CHAR(payment_date, 'Mon YYYY') " +
            "ORDER BY MIN(payment_date)", nativeQuery = true)
    List<Object[]> sumMonthlyRevenue();
}

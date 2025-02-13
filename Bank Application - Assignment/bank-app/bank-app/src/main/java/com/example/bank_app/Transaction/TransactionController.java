package com.example.bank_app.Transaction;

import com.example.bank_app.exceptionhandling.AccountNotFoundException;
import com.example.bank_app.exceptionhandling.InsufficientBalanceException;
import com.example.bank_app.exceptionhandling.InvalidTransactionAmountException;
import com.example.bank_app.exceptionhandling.InvalidTransactionIndicatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v2")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = Objects.requireNonNull(transactionService, "TransactionService must not be null");
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        if (transactions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(transactions);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @GetMapping("/transactions/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction transaction = transactionService.getTransactionById(id);
        if (transaction != null) {
            return ResponseEntity.ok(transaction);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactionsByAccountId(@PathVariable Long accountId) {
        List<Transaction> transactions = transactionService.getAllTransactionsByAccountId(accountId);
        if (transactions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(transactions);
    }
    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody Transaction transaction) {
        try {
            Transaction createdTransaction = transactionService.saveTransaction(transaction);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
        } catch (InsufficientBalanceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidTransactionIndicatorException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidTransactionAmountException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id, @RequestBody Transaction transaction) {
        Transaction existingTransaction = transactionService.getTransactionById(id);
        if (existingTransaction != null) {
            transaction.setId(id);
            try {
                Transaction updatedTransaction = transactionService.saveTransaction(transaction);
                return ResponseEntity.ok(updatedTransaction);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

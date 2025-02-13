package com.example.bank_app.Transaction;

import com.example.bank_app.Account.Account;
import com.example.bank_app.Account.AccountRepository;
import com.example.bank_app.Balance.Balance;
import com.example.bank_app.Balance.BalanceRepository;
import com.example.bank_app.exceptionhandling.AccountNotFoundException;
import com.example.bank_app.exceptionhandling.InsufficientBalanceException;
import com.example.bank_app.exceptionhandling.InvalidTransactionAmountException;
import com.example.bank_app.exceptionhandling.InvalidTransactionIndicatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository,
    BalanceRepository balanceRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

//    public List<Transaction> getAllTransactionsV2(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        return transactionRepository.findAll(pageable).getContent();
//    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElse(null);
    }

    public List<Transaction> getAllTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

//    public List<Transaction> getAllTransactionsByAccountIdV2(Long accountId, int page, int size) {
//        List<Transaction> allTransactions = transactionRepository.findByAccountId(accountId);
//
//        Pageable pageable = PageRequest.of(page, size);
//        int start = Math.min((int) pageable.getOffset(), allTransactions.size());
//        int end = Math.min((start + pageable.getPageSize()), allTransactions.size());
//
//        return allTransactions.subList(start, end);
//    }

    public Transaction saveTransaction(Transaction transaction) throws Exception {
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException("Transaction amount must be greater than zero.");
        }

        Account account = transaction.getAccount();
        Long senderAccountId = transaction.getAccount().getId();
        Optional<Account> anotherTransactionAccount = accountRepository.findById(senderAccountId);
        Account optionalAcc = anotherTransactionAccount.get();
        Balance balance = balanceRepository.findByAccount(account);

        if (balance == null) {
            throw new AccountNotFoundException("Balance not found for the account");
        }

        if ("DB".equals(transaction.getIndicator())) {
            if (balance.getAmount().compareTo(transaction.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance for the transaction");
            }
            balance.setAmount(balance.getAmount().subtract(transaction.getAmount()));
        } else if ("CR".equals(transaction.getIndicator())) {
            balance.setAmount(balance.getAmount().add(transaction.getAmount()));
        } else {
            throw new InvalidTransactionIndicatorException("Invalid transaction indicator");
        }

        if (transaction.getReceiverAccountNumber() != null) {
            Account receiverAccount = accountRepository.findByAccountNumber(transaction.getReceiverAccountNumber());
            if (receiverAccount == null) {
                throw new AccountNotFoundException("Receiver account not found");
            }
            Balance receiverBalance = balanceRepository.findByAccount(receiverAccount);
            if (receiverBalance == null) {
                throw new AccountNotFoundException("Balance not found for the receiver account");
            }
            receiverBalance.setAmount(receiverBalance.getAmount().add(transaction.getAmount()));
            balanceRepository.save(receiverBalance);

            Transaction receiverTransaction = new Transaction();
            receiverTransaction.setAccount(receiverAccount);
            receiverTransaction.setAmount(transaction.getAmount());
            receiverTransaction.setIndicator("CR");
            receiverTransaction.setReceiverAccountNumber(optionalAcc.getAccountNumber());
            receiverTransaction.setDescription(transaction.getDescription());
            receiverTransaction.setDate(LocalDateTime.now());
            transactionRepository.save(receiverTransaction);
        }

        balanceRepository.save(balance);

        transaction.setDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        return transaction;
    }

    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }
}

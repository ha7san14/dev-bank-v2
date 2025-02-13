package com.example.bank_app.Account;
import com.example.bank_app.Balance.Balance;
import com.example.bank_app.Balance.BalanceRepository;
import com.example.bank_app.Transaction.Transaction;
import com.example.bank_app.Transaction.TransactionRepository;
import com.example.bank_app.User.User;
import com.example.bank_app.User.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public AccountService(AccountRepository accountRepository, UserRepository userRepository, BalanceRepository balanceRepository,
    TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

//    public List<Account> getAllAccountsV2(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        return accountRepository.findAll(pageable).getContent();
//    }


    public Account getAccountById(Long id) {
        return accountRepository.findById(id).orElse(null);
    }
    public Account getAccountByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            List<Transaction> transaction = transactionRepository.findByAccountId(account.getId());
            if (transaction != null) {
                transactionRepository.deleteAll(transaction);
            }
            Balance balance = balanceRepository.findByAccountId(account.getId());
            if (balance != null) {
                balanceRepository.delete(balance);
            }

            User user = account.getUser();
            accountRepository.deleteById(id);
            if (user != null) {
                userRepository.deleteById(user.getId());
            }
        }
    }
}

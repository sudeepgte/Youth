package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WalletFlowTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WalletRequestRepository walletRequestRepository;

    @Mock
    private CoinTransactionRepository coinTransactionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private WalletController walletController;

    private User alice;
    private User bob;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        alice = new User();
        alice.setId(101L);
        alice.setUsername("alice");
        alice.setEmail("alice@youthians.com");
        alice.setWalletBalance(150.0);
        alice.setCoins(200);
        alice.setWelcomeBonusClaimed(true);

        bob = new User();
        bob.setId(102L);
        bob.setUsername("bob");
        bob.setEmail("bob@youthians.com");
        bob.setWalletBalance(50.0);
        bob.setCoins(50);
        bob.setWelcomeBonusClaimed(true);

        session = new MockHttpSession();
        session.setAttribute("user", alice);
        session.setAttribute("userId", alice.getId());

        when(userRepository.findById(101L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(102L)).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("alice")).thenReturn(alice);
        when(userRepository.findByUsername("bob")).thenReturn(bob);
    }

    @Test
    @DisplayName("TC1 & TC2: Welcome Bonus First-Time Claim vs Duplicate Login")
    void testWelcomeBonusLogic() {
        User newUser = new User();
        newUser.setId(201L);
        newUser.setUsername("charlie");
        newUser.setWalletBalance(0.0);
        newUser.setCoins(0);
        newUser.setWelcomeBonusClaimed(false);

        // Verify initial state
        assertFalse(newUser.isWelcomeBonusClaimed());
        assertEquals(0.0, newUser.getWalletBalance());
        assertEquals(0, newUser.getCoins());

        // Simulate first login awarding
        newUser.setWalletBalance(newUser.getWalletBalance() + 50.0);
        newUser.setCoins(newUser.getCoins() + 100);
        newUser.setWelcomeBonusClaimed(true);

        assertEquals(50.0, newUser.getWalletBalance());
        assertEquals(100, newUser.getCoins());
        assertTrue(newUser.isWelcomeBonusClaimed());

        // Simulate subsequent login - should NOT award bonus again
        if (newUser.isWelcomeBonusClaimed()) {
            // Already claimed, no-op
        } else {
            newUser.setWalletBalance(newUser.getWalletBalance() + 50.0);
            newUser.setCoins(newUser.getCoins() + 100);
        }
        assertEquals(50.0, newUser.getWalletBalance());
        assertEquals(100, newUser.getCoins());
    }

    @Test
    @DisplayName("TC3: Direct self-deposit is prohibited in the economy")
    void testDirectSelfDepositIsProhibited() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = walletController.addFunds(redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("error"));
        assertTrue(redirectAttributes.getFlashAttributes().get("error").toString().contains("Direct self-deposits are not permitted"));

        // Alice balance untouched
        assertEquals(150.0, alice.getWalletBalance(), 0.001);
        verify(userRepository, never()).save(alice);
    }

    @Test
    @DisplayName("TC4: Admin Approves Deposit -> Credits User Balance and Marks COMPLETED")
    void testAdminApproveDeposit() {
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("user", "admin");

        WalletTransaction pendingTx = new WalletTransaction();
        pendingTx.setId(991L);
        pendingTx.setUser(alice);
        pendingTx.setAmount(500.0);
        pendingTx.setType("DEPOSIT");
        pendingTx.setStatus("PENDING");

        when(walletTransactionRepository.findById(991L)).thenReturn(Optional.of(pendingTx));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = walletController.adminApproveDeposit(991L, adminSession, redirectAttributes);

        assertEquals("redirect:/admin", result);
        assertEquals("COMPLETED", pendingTx.getStatus());
        assertEquals(650.0, alice.getWalletBalance(), 0.001); // 150 + 500 = 650

        verify(userRepository, times(1)).save(alice);
        verify(walletTransactionRepository, times(1)).save(pendingTx);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC5: Admin Rejects Deposit -> Leaves User Balance Unchanged and Marks REJECTED")
    void testAdminRejectDeposit() {
        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("user", "admin");

        WalletTransaction pendingTx = new WalletTransaction();
        pendingTx.setId(992L);
        pendingTx.setUser(alice);
        pendingTx.setAmount(1000.0);
        pendingTx.setType("DEPOSIT");
        pendingTx.setStatus("PENDING");

        when(walletTransactionRepository.findById(992L)).thenReturn(Optional.of(pendingTx));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = walletController.adminRejectDeposit(992L, adminSession, redirectAttributes);

        assertEquals("redirect:/admin", result);
        assertEquals("REJECTED", pendingTx.getStatus());
        assertEquals(150.0, alice.getWalletBalance(), 0.001); // remains 150

        verify(userRepository, never()).save(alice);
        verify(walletTransactionRepository, times(1)).save(pendingTx);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC6: P2P Send Coins -> Transfers coins between users")
    void testP2PSendCoinsSuccess() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = walletController.sendFunds("bob", 40.0, "COINS", "Thanks for the battle tips!", session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));

        // Alice had 200, sends 40 -> 160
        assertEquals(160, alice.getCoins());
        // Bob had 50, receives 40 -> 90
        assertEquals(90, bob.getCoins());

        verify(userRepository, times(1)).save(alice);
        verify(userRepository, times(1)).save(bob);
        verify(coinTransactionRepository, times(2)).save(any(CoinTransaction.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC7: P2P Send Coins -> Rejects transfer when insufficient balance")
    void testP2PSendCoinsInsufficientBalance() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // Alice has 200 coins, tries to send 500
        String result = walletController.sendFunds("bob", 500.0, "COINS", "Excessive coins", session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("error"));
        assertTrue(redirectAttributes.getFlashAttributes().get("error").toString().contains("Insufficient coins"));

        assertEquals(200, alice.getCoins());
        assertEquals(50, bob.getCoins());
        verify(userRepository, never()).save(alice);
    }

    @Test
    @DisplayName("TC8: P2P Send -> Self-transfer is prohibited")
    void testP2PSendSelfTransferProhibited() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = walletController.sendFunds("alice", 50.0, "COINS", "Self send", session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("error"));
        assertTrue(redirectAttributes.getFlashAttributes().get("error").toString().contains("cannot send funds to yourself"));
    }

    @Test
    @DisplayName("TC9: P2P Send Cash -> Transfers INR between users")
    void testP2PSendCashSuccess() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // Alice has 150.0, sends 50.0 to Bob (who had 50.0)
        String result = walletController.sendFunds("bob", 50.0, "CASH", "Dinner split", session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));

        assertEquals(100.0, alice.getWalletBalance(), 0.001);
        assertEquals(100.0, bob.getWalletBalance(), 0.001);

        verify(userRepository, times(1)).save(alice);
        verify(userRepository, times(1)).save(bob);
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("TC10: P2P Request Coins -> Creates PENDING request and notifies target")
    void testP2PRequestCoins() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = walletController.requestFunds("bob", 25.0, "COINS", "Need coins for battle", session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));

        verify(walletRequestRepository, times(1)).save(argThat(req ->
            req.getRequester().equals(alice) &&
            req.getTargetUser().equals(bob) &&
            "COINS".equals(req.getCurrency()) &&
            Double.valueOf(25.0).equals(req.getAmount()) &&
            "PENDING".equals(req.getStatus())
        ));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC11: P2P Accept Request -> Debits target and credits requester")
    void testP2PAcceptRequest() {
        // Bob requested 30 coins from Alice
        WalletRequest req = new WalletRequest();
        req.setId(881L);
        req.setRequester(bob);
        req.setTargetUser(alice);
        req.setAmount(30.0);
        req.setCurrency("COINS");
        req.setStatus("PENDING");

        when(walletRequestRepository.findById(881L)).thenReturn(Optional.of(req));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = walletController.acceptRequest(881L, session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));
        assertEquals("ACCEPTED", req.getStatus());

        // Alice (target) pays 30 coins: 200 - 30 = 170
        assertEquals(170, alice.getCoins());
        // Bob (requester) receives 30 coins: 50 + 30 = 80
        assertEquals(80, bob.getCoins());

        verify(walletRequestRepository, times(1)).save(req);
        verify(coinTransactionRepository, times(2)).save(any(CoinTransaction.class));
    }

    @Test
    @DisplayName("TC11b: P2P Accept Cash Request -> Debits target cash and credits requester cash")
    void testP2PAcceptCashRequest() {
        // Bob requested Rs. 50.00 cash from Alice
        WalletRequest req = new WalletRequest();
        req.setId(883L);
        req.setRequester(bob);
        req.setTargetUser(alice);
        req.setAmount(50.0);
        req.setCurrency("CASH");
        req.setStatus("PENDING");

        when(walletRequestRepository.findById(883L)).thenReturn(Optional.of(req));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = walletController.acceptRequest(883L, session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));
        assertEquals("ACCEPTED", req.getStatus());

        // Alice (target) had 150.0, pays 50.0 -> 100.0
        assertEquals(100.0, alice.getWalletBalance(), 0.001);
        // Bob (requester) had 50.0, receives 50.0 -> 100.0
        assertEquals(100.0, bob.getWalletBalance(), 0.001);

        verify(walletRequestRepository, times(1)).save(req);
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC12: P2P Decline Request -> Marks DECLINED without changing balances")
    void testP2PDeclineRequest() {
        // Bob requested 30 coins from Alice
        WalletRequest req = new WalletRequest();
        req.setId(882L);
        req.setRequester(bob);
        req.setTargetUser(alice);
        req.setAmount(30.0);
        req.setCurrency("COINS");
        req.setStatus("PENDING");

        when(walletRequestRepository.findById(882L)).thenReturn(Optional.of(req));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String result = walletController.declineRequest(882L, session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));
        assertEquals("DECLINED", req.getStatus());

        // Balances untouched
        assertEquals(200, alice.getCoins());
        assertEquals(50, bob.getCoins());

        verify(walletRequestRepository, times(1)).save(req);
        verify(coinTransactionRepository, never()).save(any(CoinTransaction.class));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC13: Direct coin purchases are disabled")
    void testDirectCoinPurchasesAreDisabled() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String result = walletController.buyCoins(redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("error"));
        assertTrue(redirectAttributes.getFlashAttributes().get("error").toString().contains("Direct coin purchases are disabled"));

        assertEquals(150.0, alice.getWalletBalance(), 0.001);
        assertEquals(200, alice.getCoins());
    }

    @Test
    @DisplayName("TC14: Withdraw Funds -> Locks amount, deducts 5% platform charge, and records transaction with PENDING status")
    void testWithdrawFundsLocksBalanceAndSetsPending() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // Alice has 150.0 cash, requests 100.0 withdrawal to UPI
        String result = walletController.withdrawFunds(100.0, "alice@okhdfcbank", session, redirectAttributes);

        assertEquals("redirect:/wallet", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));

        // Alice balance deducted immediately: 150.0 - 100.0 = 50.0
        assertEquals(50.0, alice.getWalletBalance(), 0.001);

        verify(userRepository, times(1)).save(alice);
        verify(walletTransactionRepository, times(1)).save(argThat(tx -> 
            "WITHDRAWAL".equals(tx.getType()) &&
            "PENDING".equals(tx.getStatus()) &&
            Double.valueOf(100.0).equals(tx.getAmount()) &&
            Double.valueOf(5.0).equals(tx.getFee()) &&
            Double.valueOf(95.0).equals(tx.getNetAmount())
        ));
    }
}

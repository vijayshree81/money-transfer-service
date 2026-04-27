package com.mybank.moneytransfer;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.moneytransfer.dto.BalanceResult;
import com.mybank.moneytransfer.dto.TransferResult;
import com.mybank.moneytransfer.error.OverDraftException;
import com.mybank.moneytransfer.entity.Account;
import com.mybank.moneytransfer.dto.TransferRequest;
import com.mybank.moneytransfer.repository.AccountRepository;
import com.mybank.moneytransfer.service.FundTransferLogService;
import com.mybank.moneytransfer.service.MoneyTransactionService;

/**
 * BDD-style integration tests organized as a sequential business flow:
 *
 *   1. Account Creation     — open new accounts (happy + error paths)
 *   2. Account Retrieval    — look up existing accounts
 *   3. Unsupported Ops      — verify rejected HTTP methods
 *   4. Fund Transfer         — move money between accounts (happy + error paths)
 *   5. Account Audit Logs   — verify account-level audit trail
 *   6. Transaction Audit Logs — verify transfer-level audit trail
 */
@SpringBootTest
@AutoConfigureMockMvc
@MockitoSettings(strictness = Strictness.LENIENT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class MyBankMoneyTransferTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    AccountRepository accountRepository;
    @Mock
    FundTransferLogService fundTransferLogService;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbcTemplate;
    private MoneyTransactionService moneyTxnService;
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    Map<String, Account> testData;
    final String ACCT_BASE_URL = "/mybank/v1/account";
    final String TXN_BASE_URL = "/mybank/v1/transaction";
    final String AUDIT_BASE_URL = "/mybank/v1/audit";

    @BeforeEach
    void setup() {
        moneyTxnService = new MoneyTransactionService(accountRepository, fundTransferLogService, restTemplate);
        jdbcTemplate.execute("DELETE FROM FUND_TRANSFER_LOG");
        jdbcTemplate.execute("DELETE FROM ACCOUNT_AUDIT_LOG");
        jdbcTemplate.execute("DELETE FROM ACCOUNT");
        testData = getTestData();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    private Account createAccountViaApi(Account account) throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(post(ACCT_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(account)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(),
                Account.class);
    }

    // ───────────────────────────────────────────────────────────────
    // 1. Account Creation
    // ───────────────────────────────────────────────────────────────

    @Nested
    @Order(1)
    @DisplayName("1. Account Creation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AccountCreation {

        @Test
        @Order(1)
        @DisplayName("1.1 Given valid account data, when POST to /account, then return 201 with created account")
        void shouldCreateAccountWhenDataIsValid() throws Exception {
            // Given
            var expectedRecord = testData.get("account1");
            given(accountRepository.findByAccountId(expectedRecord.getAccountId()))
                    .willReturn(Optional.of(expectedRecord));

            // When
            var actualRecord = createAccountViaApi(expectedRecord);

            // Then
            then(new ReflectionEquals(expectedRecord, "accountId").matches(actualRecord)).isTrue();
            then(accountRepository.findByAccountId(actualRecord.getAccountId()).get().getAccountId())
                    .isEqualTo(expectedRecord.getAccountId());
        }

        @Test
        @Order(2)
        @DisplayName("1.2 Given null balance, when POST to /account, then default balance to zero")
        void shouldDefaultBalanceToZeroWhenNull() throws Exception {
            // Given
            var now = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());
            var nullBalanceAcct = new Account("111222333", "C", "A", now, null);

            // When
            var actualRecord = createAccountViaApi(nullBalanceAcct);

            // Then
            then(actualRecord.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @Order(3)
        @DisplayName("1.3 Given invalid account ID (<9 chars), when POST to /account, then return 400")
        void shouldRejectAccountWhenDataIsInvalid() throws Exception {
            // Given
            var invalidAccount = testData.get("invalidAcct1");

            // When
            var result = mockMvc.perform(post(ACCT_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidAccount)))
                    .andDo(print());

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }


        @Test
        @Order(4)
        @DisplayName("1.4 Given account ID exist, when POST to /account, then return 409")
        void shouldRejectAccountWhenAccountExist() throws Exception {
            // Given — create the account first
            var acct1Exists = testData.get("acct1Exists");
            mockMvc.perform(post(ACCT_BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acct1Exists)))
                    .andExpect(status().isCreated());

            // When — attempt to create the same account again
            var result = mockMvc.perform(post(ACCT_BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(acct1Exists)))
                    .andDo(print());

            // Then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @Order(5)
        @DisplayName("1.5 Given negative balance, when POST to /account, then return 400 with error message")
        void shouldRejectAccountWhenBalanceIsNegative() throws Exception {
            // Given
            var now = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());
            var negativeBalanceAcct = new Account("999888777", "C", "A", now, new BigDecimal("-500"));

            // When
            var result = mockMvc.perform(post(ACCT_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(negativeBalanceAcct)))
                    .andDo(print());

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Account balance can not be negative, value:-500"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 2. Account Retrieval
    // ───────────────────────────────────────────────────────────────

    @Nested
    @Order(2)
    @DisplayName("2. Account Retrieval")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AccountRetrieval {

        @Test
        @Order(1)
        @DisplayName("2.1 Given existing account, when GET /account/{id}/info, then return 200 with account details")
        void shouldReturnAccountWhenIdExists() throws Exception {
            // Given
            var createdAccount = createAccountViaApi(getTestData().get("account1"));

            // When
            var actualRecord = objectMapper.readValue(
                    mockMvc.perform(get(ACCT_BASE_URL + "/info").header("accountId", createdAccount.getAccountId()))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    Account.class);

            // Then
            then(actualRecord.getAccountId()).isEqualTo(createdAccount.getAccountId());
        }

        @Test
        @Order(2)
        @DisplayName("2.2 Given non-existent account ID, when GET /account/info with header, then return 404 Not Found")
        void shouldReturn404WhenIdDoesNotExist() throws Exception {
            // Given
            var nonExistentAccountId = "X0123531X";

            // When
            var result = mockMvc.perform(get(ACCT_BASE_URL + "/info").header("accountId", nonExistentAccountId));

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.httpCodeMessage").value("Not Found"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 3. Unsupported Operations
    // ───────────────────────────────────────────────────────────────

    @Nested
    @Order(3)
    @DisplayName("3. Unsupported Operations")
    class UnsupportedOperations {

        @Test
        @DisplayName("3.1 Given existing account, when PATCH or DELETE, then return 4xx Method Not Allowed")
        void shouldRejectPatchAndDeleteMethods() throws Exception {
            // Given
            var createdAccount = createAccountViaApi(getTestData().get("account1"));

            // When / Then
            mockMvc.perform(patch(ACCT_BASE_URL + "/" + createdAccount.getAccountId()))
                    .andExpect(status().is4xxClientError());

            mockMvc.perform(delete(ACCT_BASE_URL + "/" + createdAccount.getAccountId()))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 4. Fund Transfer
    // ───────────────────────────────────────────────────────────────

    @Nested
    @Order(4)
    @DisplayName("4. Fund Transfer")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FundTransfer {

        @Test
        @Order(1)
        @DisplayName("4.1 Given two accounts with sufficient balance, when transferMoney at service layer, then debit source and credit destination")
        void shouldTransferMoneyBetweenAccountsAtServiceLayer() throws Exception {
            // Given
            var acctFrom = testData.get("account1");
            var acctTo = testData.get("account2");
            var amtTransfer = new BigDecimal(15500);
            var transRequest = new TransferRequest("REQ0001", acctFrom.getAccountId(), acctTo.getAccountId(), amtTransfer);
            given(accountRepository.getAccountForUpdate(acctFrom.getAccountId())).willReturn(Optional.of(acctFrom));
            given(accountRepository.getAccountForUpdate(acctTo.getAccountId())).willReturn(Optional.of(acctTo));

            // When
            moneyTxnService.transferMoney(transRequest, "TXN-TEST-001");

            // Then
            then(acctFrom.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            then(acctTo.getBalance()).isEqualByComparingTo(new BigDecimal("25500"));
        }

        @Test
        @Order(2)
        @DisplayName("4.2 Given two accounts, when POST to /transaction with includeBalance=true, then return 202 with updated balance")
        void shouldTransferMoneyViaApi() throws Exception {
            // Given
            var acct1 = testData.get("account1");
            var acct1Rec = createAccountViaApi(acct1);
            var acct2 = testData.get("account2");
            createAccountViaApi(acct2);

            var amtTransfer = new BigDecimal("500");
            var balanceResultMock = new BalanceResult(acct1.getAccountId(), acct1.getBalance(), acct1.getBalance().subtract(amtTransfer));
            mockServer.expect(ExpectedCount.between(0, 10),
                    requestTo(new URI("http://localhost:8080/mybank/v1/account/balance")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(objectMapper.writeValueAsString(balanceResultMock)));

            var transRequest = new TransferRequest("REQ0001", acct1.getAccountId(), acct2.getAccountId(), amtTransfer, true);

            // When
            var transResult = objectMapper.readValue(
                    mockMvc.perform(post(TXN_BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transRequest)))
                            .andDo(print())
                            .andExpect(status().isAccepted())
                            .andReturn().getResponse().getContentAsString(),
                    TransferResult.class);

            // Then
            then(transResult.balanceAfterTransfer())
                    .isEqualByComparingTo(acct1Rec.getBalance().subtract(amtTransfer));
        }

        @Test
        @Order(3)
        @DisplayName("4.3 Given same source and destination account, when transferMoney, then throw BusinessException")
        void shouldRejectSameAccountTransfer() throws Exception {
            // Given
            var acct = testData.get("account1");
            var transRequest = new TransferRequest("REQ0002", acct.getAccountId(), acct.getAccountId(), new BigDecimal("100"));
            given(accountRepository.getAccountForUpdate(acct.getAccountId())).willReturn(Optional.of(acct));

            // When / Then
            thenThrownBy(() -> moneyTxnService.transferMoney(transRequest, "TXN-TEST-002"))
                    .isInstanceOf(OverDraftException.class)
                    .hasMessage("Source and destination accounts must be different");
        }

        @Test
        @Order(4)
        @DisplayName("4.4 Given insufficient balance, when transferMoney, then throw OverDraftException")
        void shouldRejectTransferWhenInsufficientBalance() throws Exception {
            // Given
            var acctFrom = testData.get("account1"); // balance: 15500
            var acctTo = testData.get("account2");
            var excessiveAmount = new BigDecimal("99999");
            var transRequest = new TransferRequest("REQ0003", acctFrom.getAccountId(), acctTo.getAccountId(), excessiveAmount);
            given(accountRepository.getAccountForUpdate(acctFrom.getAccountId())).willReturn(Optional.of(acctFrom));
            given(accountRepository.getAccountForUpdate(acctTo.getAccountId())).willReturn(Optional.of(acctTo));

            // When / Then
            thenThrownBy(() -> moneyTxnService.transferMoney(transRequest, "TXN-TEST-003"))
                    .isInstanceOf(OverDraftException.class)
                    .hasMessageContaining("does not have enough balance");
        }

        @Test
        @Order(5)
        @DisplayName("4.5 Given insufficient balance, when POST to /transaction, then return 400 with overdraft message")
        void shouldReturn400WhenOverdraftViaApi() throws Exception {
            // Given
            var acct1 = testData.get("account1");
            createAccountViaApi(acct1);
            var acct2 = testData.get("account2");
            createAccountViaApi(acct2);

            var transRequest = new TransferRequest("REQ0004", acct1.getAccountId(), acct2.getAccountId(), new BigDecimal("99999"));

            // When
            var result = mockMvc.perform(post(TXN_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transRequest)))
                    .andDo(print());

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Account with id:323434322 does not have enough balance to transfer."));
        }

        @Test
        @Order(6)
        @DisplayName("4.6 Given zero transfer amount, when POST to /transaction, then return 400 validation error")
        void shouldRejectZeroAmountTransferViaApi() throws Exception {
            // Given
            var acct1 = testData.get("account1");
            createAccountViaApi(acct1);
            var acct2 = testData.get("account2");
            createAccountViaApi(acct2);

            var transRequest = new TransferRequest("REQ0005", acct1.getAccountId(), acct2.getAccountId(), BigDecimal.ZERO);

            // When
            var result = mockMvc.perform(post(TXN_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transRequest)))
                    .andDo(print());

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @Order(7)
        @DisplayName("4.7 Given non-existent source account, when POST to /transaction, then return 404")
        void shouldReturn404WhenSourceAccountMissing() throws Exception {
            // Given
            var acct2 = testData.get("account2");
            createAccountViaApi(acct2);

            var transRequest = new TransferRequest("REQ0006", "999999999", acct2.getAccountId(), new BigDecimal("100"));

            // When
            var result = mockMvc.perform(post(TXN_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transRequest)))
                    .andDo(print());

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @Order(8)
        @DisplayName("4.8 Given same source and destination, when POST to /transaction, then return 400")
        void shouldReturn400WhenSameAccountViaApi() throws Exception {
            // Given
            var acct1 = testData.get("account1");
            createAccountViaApi(acct1);

            var transRequest = new TransferRequest("REQ0007", acct1.getAccountId(), acct1.getAccountId(), new BigDecimal("100"));

            // When
            var result = mockMvc.perform(post(TXN_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transRequest)))
                    .andDo(print());

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Source and destination accounts must be different"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 5. Account Audit Logs
    // ───────────────────────────────────────────────────────────────

    @Nested
    @Order(5)
    @DisplayName("5. Account Audit Logs")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AccountAuditLogs {

        @Test
        @Order(1)
        @DisplayName("5.1 Given no accounts created, when GET /audit/account/logs, then return empty array")
        void shouldReturnEmptyAccountAuditLogs() throws Exception {
            // When
            var result = mockMvc.perform(get(AUDIT_BASE_URL + "/account/logs"));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @Order(2)
        @DisplayName("5.2 Given account created, when GET /audit/account/logs, then return CREATE/SUCCESS entry")
        void shouldReturnAuditLogsAfterAccountCreation() throws Exception {
            // Given
            createAccountViaApi(testData.get("account1"));

            // When
            var result = mockMvc.perform(get(AUDIT_BASE_URL + "/account/logs"));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].accountId").value("323434322"))
                    .andExpect(jsonPath("$[0].action").value("CREATE"))
                    .andExpect(jsonPath("$[0].status").value("SUCCESS"));
        }

        @Test
        @Order(3)
        @DisplayName("5.3 Given multiple accounts, when GET /audit/account/{id}/logs, then return only matching account's logs")
        void shouldReturnAuditLogsByAccountId() throws Exception {
            // Given
            createAccountViaApi(testData.get("account1"));
            createAccountViaApi(testData.get("account2"));

            // When
            var result = mockMvc.perform(get(AUDIT_BASE_URL + "/account/323434322/logs"));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].accountId").value("323434322"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 6. Transaction Audit Logs
    // ───────────────────────────────────────────────────────────────

    @Nested
    @Order(6)
    @DisplayName("6. Transaction Audit Logs")
    class TransactionAuditLogs {

        @Test
        @DisplayName("6.1 Given successful fund transfer, when GET /audit/transaction/logs, then return SUCCESS entry with correct accounts")
        void shouldReturnTransactionLogsAfterTransfer() throws Exception {
            // Given
            createAccountViaApi(testData.get("account1"));
            createAccountViaApi(testData.get("account2"));

            var transRequest = new TransferRequest("REQ0010", "323434322", "654444322", new BigDecimal("100"));
            mockMvc.perform(post(TXN_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transRequest)))
                    .andExpect(status().isAccepted());

            // When
            var result = mockMvc.perform(get(AUDIT_BASE_URL + "/transaction/logs"));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].accountFromId").value("323434322"))
                    .andExpect(jsonPath("$[0].accountToId").value("654444322"))
                    .andExpect(jsonPath("$[0].status").value("SUCCESS"));
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Shared test data
    // ───────────────────────────────────────────────────────────────

    private Map<String, Account> getTestData() {
        var now = new Timestamp(GregorianCalendar.getInstance().getTimeInMillis());
        return Map.of(
                "account1", new Account("323434322", "C", "A", now, new BigDecimal("15500")),
                "account2", new Account("654444322", "S", "A", now, new BigDecimal("10000")),
                "invalidAcct1", new Account("121211", "S", "A", now, new BigDecimal("12000")),
                "acct1Exists", new Account("323434322", "C", "A", now, new BigDecimal("12000"))
        );
    }
}

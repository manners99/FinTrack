package org.vaadin.example.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.vaadin.example.model.User;
import org.vaadin.example.model.Budget;
import org.vaadin.example.service.BudgetService;
import org.vaadin.example.service.SessionService;
import org.vaadin.example.service.UserService;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.provider.Query;

import java.math.BigDecimal;
import java.util.List;

class BudgetViewTests {

    @Mock
    private BudgetService budgetService;

    @Mock
    private SessionService sessionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private BudgetView budgetView;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testListBudgets() {
        Long userId = 1L;
        Budget budget = new Budget();
        budget.setName("name");
        budget.setAmount(new BigDecimal("5000"));
        when(sessionService.getLoggedInUserId()).thenReturn(userId);
        when(budgetService.getBudgetsByUserId(userId)).thenReturn(List.of(budget));

        budgetView.listBudgets();

        Mockito.verify(budgetService).getBudgetsByUserId(userId);
        assertEquals(1, budgetView.grid.getDataProvider().size(new Query<>()));
    }

    @ParameterizedTest
    @CsvSource({
        "'name', '5000'",
        "'name', ''",
        "'', 5000",
        "'', ''"
    })
    void testClearForm(String name, String amount) {
        budgetView.nameField.setValue(name);
        budgetView.amountField.setValue(amount);

        budgetView.clearForm();

        assertEquals("", budgetView.nameField.getValue());
        assertEquals("", budgetView.amountField.getValue());
    }

    @Test
    void testAddBudgetValidAmount() {
        String name = "name";
        String amountText = "5000";
        BigDecimal amount = new BigDecimal(amountText);
        
        try (var notification = Mockito.mockStatic(Notification.class)) {
            
            budgetView.nameField.setValue(name);
            budgetView.amountField.setValue(amountText);
    
            when(sessionService.getLoggedInUserId()).thenReturn(1L);
            when(userService.findUserById(1L)).thenReturn(new User());
    
            budgetView.addBudget();
    
            Mockito.verify(budgetService).addBudget(any(Budget.class));
            assertEquals("", budgetView.nameField.getValue());
            assertEquals("", budgetView.amountField.getValue());
            notification.verify(() -> Notification.show("Budget added successfully", 3000, Notification.Position.TOP_CENTER));
        }
    }

    @Test
    void testAddBudgetInvalidAmount() {
        String name = "name";
        String amountText = "invalid";
        
        try (var notification = Mockito.mockStatic(Notification.class)) {
            
            budgetView.nameField.setValue(name);
            budgetView.amountField.setValue(amountText);
    
            budgetView.addBudget();
    
            assertThrows(NumberFormatException.class, () -> new BigDecimal(amountText));
            Mockito.verify(budgetService, never()).addBudget(any(Budget.class));
            notification.verify(() -> Notification.show("Please enter a valid amount", 3000, Notification.Position.TOP_CENTER));
        }
    }

    @Test
    void testDeleteBudgetWhenBudgetSelected() {
        Budget budget = new Budget();
        budget.setId(1L);
        budget.setName("name");
        budget.setAmount(new BigDecimal("5000"));
        budgetView.grid.select(budget);

        try (var notification = Mockito.mockStatic(Notification.class)) {
            budgetView.deleteBudget();
    
            Mockito.verify(budgetService).deleteBudget(1L);
            assert(budgetView.grid.getDataProvider().fetch(new Query<>()).count() == 0);
            notification.verify(() -> Notification.show("Budget deleted successfully", 3000, Notification.Position.TOP_CENTER));
        }
    }

    @Test 
    void testDeleteBudgetWhenMultipleSelected() {
        Budget budget1 = new Budget();
        budget1.setId(1L);
        budget1.setName("name1");
        budget1.setAmount(new BigDecimal("5000"));
        Budget budget2 = new Budget();
        budget2.setId(2L);
        budget2.setName("name2");
        budget2.setAmount(new BigDecimal("5000"));
        budgetView.grid.select(budget1);

        try (var notification = Mockito.mockStatic(Notification.class)) {
            budgetView.deleteBudget();
    
            Mockito.verify(budgetService).deleteBudget(1L);
            Mockito.verify(budgetService, never()).deleteBudget(2L);
            notification.verify(() -> Notification.show("Budget deleted successfully", 3000, Notification.Position.TOP_CENTER));
        }
    }

    @Test
    void testDeleteBudgetWhenNoBudgetSelected() {
        try (var notification = Mockito.mockStatic(Notification.class)) {
            budgetView.deleteBudget();
    
            Mockito.verify(budgetService, never()).deleteBudget(any(Long.class));
            notification.verify(() -> Notification.show("Please select a budget to delete", 3000, Notification.Position.TOP_CENTER));
        }
    }
    
}
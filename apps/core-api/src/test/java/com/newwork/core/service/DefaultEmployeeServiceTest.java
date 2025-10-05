package com.newwork.core.service;

import com.newwork.core.domain.Employee;
import com.newwork.core.repo.EmployeeRepository;
import com.newwork.core.service.impl.DefaultEmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultEmployeeServiceTest {

    @Mock
    private EmployeeRepository repo;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new DefaultEmployeeService(repo);
    }

    private static Employee emp(UUID id, String first, String last, Integer version) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName(first);
        e.setLastName(last);
        e.setVersion(version);
        return e;
    }

    @Test
    void getAllEmployees_returnsListFromRepo() {
        var list = List.of(emp(UUID.randomUUID(), "Alice", "Ng", 0));
        when(repo.findAll()).thenReturn(list);

        var result = service.getAllEmployees();

        assertThat(result).isSameAs(list);
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findById_found_returnsEmployee() {
        UUID id = UUID.randomUUID();
        var e = emp(id, "Bob", "Ionescu", 1);
        when(repo.findById(id)).thenReturn(Optional.of(e));

        var result = service.findById(id);

        assertThat(result).containsSame(e);
        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        var result = service.findById(id);

        assertThat(result).isEmpty();
        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void save_delegatesToRepo_andReturnsSaved() {
        var toSave = emp(null, "Dana", "Pop", null);
        var saved  = emp(UUID.randomUUID(), "Dana", "Pop", 0);
        when(repo.save(toSave)).thenReturn(saved);

        var result = service.save(toSave);

        assertThat(result).isSameAs(saved);

        ArgumentCaptor<Employee> cap = ArgumentCaptor.forClass(Employee.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue()).isSameAs(toSave);

        verifyNoMoreInteractions(repo);
    }

    @Test
    void delete_delegatesToRepo() {
        var e = emp(UUID.randomUUID(), "Z", "Q", 5);

        service.delete(e);

        verify(repo).delete(e);
        verifyNoMoreInteractions(repo);
    }
}

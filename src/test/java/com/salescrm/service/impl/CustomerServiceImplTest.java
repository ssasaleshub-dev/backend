package com.salescrm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.salescrm.domain.Customer;
import com.salescrm.repository.CustomerRepository;
import com.salescrm.service.dto.CustomerDTO;
import com.salescrm.service.mapper.CustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, customerMapper);
    }

    @Test
    void shouldCreateNewCustomerWhenNotExists() {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullName("John Doe");
        dto.setPhoneNumber("+919876543210");
        dto.setLocation("Chennai");

        Customer savedEntity = new Customer();
        savedEntity.setId(100L);
        savedEntity.setFullName("John Doe");
        savedEntity.setPhoneNumber("+919876543210");
        savedEntity.setLocation("Chennai");

        when(customerRepository.findByPhoneNumber("+919876543210")).thenReturn(Mono.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(savedEntity));

        CustomerDTO result = customerService.save(dto).block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getPhoneNumber()).isEqualTo("+919876543210");

        verify(customerRepository).findByPhoneNumber("+919876543210");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldReuseAndUpsertExistingCustomerWhenPhoneNumberAlreadyExists() {
        CustomerDTO dto = new CustomerDTO();
        dto.setFullName("Existing Name Updated");
        dto.setPhoneNumber("+919876543210");
        dto.setLocation("Coimbatore");

        Customer existing = new Customer();
        existing.setId(50L);
        existing.setFullName("Old Name");
        existing.setPhoneNumber("+919876543210");
        existing.setLocation("Old City");

        when(customerRepository.findByPhoneNumber("+919876543210")).thenReturn(Mono.just(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CustomerDTO result = customerService.save(dto).block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getFullName()).isEqualTo("Existing Name Updated");
        assertThat(result.getLocation()).isEqualTo("Coimbatore");

        verify(customerRepository).findByPhoneNumber("+919876543210");
        verify(customerRepository).save(existing);
    }
}

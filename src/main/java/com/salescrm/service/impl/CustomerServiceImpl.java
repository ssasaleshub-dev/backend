package com.salescrm.service.impl;

import com.salescrm.repository.CustomerRepository;
import com.salescrm.service.CustomerService;
import com.salescrm.service.dto.CustomerDTO;
import com.salescrm.service.mapper.CustomerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.salescrm.domain.Customer}.
 */
@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;

    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public Mono<CustomerDTO> save(CustomerDTO customerDTO) {
        LOG.debug("Request to save Customer : {}", customerDTO);
        if (customerDTO.getPhoneNumber() != null && !customerDTO.getPhoneNumber().isBlank()) {
            String cleanPhone = customerDTO.getPhoneNumber().trim();
            return customerRepository
                .findByPhoneNumber(cleanPhone)
                .switchIfEmpty(
                    Mono.defer(() -> {
                        if (customerDTO.getEmail() != null && !customerDTO.getEmail().isBlank()) {
                            return customerRepository.findByEmail(customerDTO.getEmail().trim().toLowerCase());
                        }
                        return Mono.empty();
                    })
                )
                .flatMap(existingCustomer -> {
                    customerMapper.partialUpdate(existingCustomer, customerDTO);
                    return customerRepository.save(existingCustomer);
                })
                .switchIfEmpty(Mono.defer(() -> customerRepository.save(customerMapper.toEntity(customerDTO))))
                .map(customerMapper::toDto);
        }
        return customerRepository.save(customerMapper.toEntity(customerDTO)).map(customerMapper::toDto);
    }

    @Override
    public Mono<CustomerDTO> update(CustomerDTO customerDTO) {
        LOG.debug("Request to update Customer : {}", customerDTO);
        return customerRepository.save(customerMapper.toEntity(customerDTO)).map(customerMapper::toDto);
    }

    @Override
    public Mono<CustomerDTO> partialUpdate(CustomerDTO customerDTO) {
        LOG.debug("Request to partially update Customer : {}", customerDTO);

        return customerRepository
            .findById(customerDTO.getId())
            .map(existingCustomer -> {
                customerMapper.partialUpdate(existingCustomer, customerDTO);

                return existingCustomer;
            })
            .flatMap(customerRepository::save)
            .map(customerMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<CustomerDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Customers");
        return customerRepository.findAllBy(pageable).map(customerMapper::toDto);
    }

    public Mono<Long> countAll() {
        return customerRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<CustomerDTO> findOne(Long id) {
        LOG.debug("Request to get Customer : {}", id);
        return customerRepository.findById(id).map(customerMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Customer : {}", id);
        return customerRepository.deleteById(id);
    }
}

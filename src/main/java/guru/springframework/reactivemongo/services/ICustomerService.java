package guru.springframework.reactivemongo.services;

import guru.springframework.reactivemongo.model.CustomerDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ICustomerService {

    Flux<CustomerDTO> listCustomers();
    Mono<CustomerDTO> getById(String id);

    Mono<CustomerDTO> saveNewCustomer(CustomerDTO customerDTO);
    Mono<CustomerDTO> saveNewCustomer(Mono<CustomerDTO> customerDTO);

    Mono<CustomerDTO> updateCustomer(String customerId, CustomerDTO customerDTO);

    Mono<CustomerDTO> patchCustomer(String customerId, CustomerDTO customerDTO);

    Mono<Void> deleteCustomerById(String customerId);

    Mono<CustomerDTO> findFirstByCustomerName(String customerName);

}

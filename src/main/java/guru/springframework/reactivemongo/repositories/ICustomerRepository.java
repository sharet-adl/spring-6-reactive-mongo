package guru.springframework.reactivemongo.repositories;

import guru.springframework.reactivemongo.domain.Customer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface ICustomerRepository extends ReactiveMongoRepository<Customer, String> {
    Mono<Customer> findFirstByCustomerName(String customerName);
}

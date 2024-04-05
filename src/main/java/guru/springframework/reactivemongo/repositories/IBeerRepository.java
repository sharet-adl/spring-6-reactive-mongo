package guru.springframework.reactivemongo.repositories;

import guru.springframework.reactivemongo.domain.Beer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IBeerRepository extends ReactiveMongoRepository<Beer, String> {

    // because the DB data is not unique, it will return only the first one. Will not throw an error if not found
    Mono<Beer> findFirstByBeerName(String beerName);

    Flux<Beer> findByBeerStyle(String beerStyle);
}

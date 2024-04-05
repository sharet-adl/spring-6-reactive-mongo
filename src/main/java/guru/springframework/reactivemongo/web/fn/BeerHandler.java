package guru.springframework.reactivemongo.web.fn;

import guru.springframework.reactivemongo.model.BeerDTO;
import guru.springframework.reactivemongo.services.IBeerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BeerHandler {
    private final IBeerService beerService;
    private final Validator validator;

    // Bean property validation
    // BINDING = when object is passed in ( binding to Bean properties )
    private void validate(BeerDTO beerDTO) {
         Errors errors = new BeanPropertyBindingResult(beerDTO, "beerDto");
         validator.validate(beerDTO, errors);

         if(errors.hasErrors()) {
             throw new ServerWebInputException(errors.toString());
         }
    }

    public Mono<ServerResponse> listBeers(ServerRequest serverRequest) {
        Flux<BeerDTO> flux;
        if(serverRequest.queryParam("beerStyle").isPresent()) {
            flux = beerService.findByBeerStyle(serverRequest.queryParam("beerStyle").get());
        } else {
            flux = beerService.listBeers();
        }

        return ServerResponse.ok()
                .body(flux, BeerDTO.class);
    }

    public Mono<ServerResponse> getBeerById(ServerRequest request) {
        return ServerResponse.ok()
                .body(beerService.getById(request.pathVariable("beerId"))
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND))),
                        BeerDTO.class);
    }

    // call flatMap because it is returning a new Publisher .. avoid the additional wrapping/nested, eg Mono<Mono<R>>
    //   .map() would produce itself a Mono<R>
    public Mono<ServerResponse> createNewBeer(ServerRequest request) {
        return beerService.saveBeer(request.bodyToMono(BeerDTO.class).doOnNext(this::validate))
                .flatMap(beerDTO -> ServerResponse
                                .created(UriComponentsBuilder
                                        .fromPath(BeerRouterConfig.BEER_PATH_ID)
                                        .build(beerDTO.getId()))
                                .build());
    }

    public Mono<ServerResponse> updateBeerById(ServerRequest request) {
        return request.bodyToMono(BeerDTO.class)
                .doOnNext(this::validate)
                .flatMap(beerDTO ->
                        beerService.updateBeer(request.pathVariable("beerId"), beerDTO))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(savedDto -> ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> patchBeerById(ServerRequest request) {
        return request.bodyToMono(BeerDTO.class)
                .doOnNext(this::validate)
                .flatMap(beerDTO ->
                    beerService.patchBeer(request.pathVariable("beerId"), beerDTO)
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(savedDto -> ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> deleteBeerById(ServerRequest request) {
        return beerService.getById(request.pathVariable("beerId"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(beerDto -> beerService.deleteBeerById(request.pathVariable("beerId")))
                    .then(ServerResponse.noContent().build());
    }
}

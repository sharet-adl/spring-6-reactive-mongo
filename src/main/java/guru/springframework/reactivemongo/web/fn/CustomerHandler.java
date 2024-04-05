package guru.springframework.reactivemongo.web.fn;

import guru.springframework.reactivemongo.model.CustomerDTO;
import guru.springframework.reactivemongo.services.ICustomerService;
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
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomerHandler {
    private final ICustomerService customerService;
    private final Validator validator;

    // Bean property validation
    // BINDING = when object is passed in ( binding to Bean properties )
    private void validate(CustomerDTO customerDTO) {
         Errors errors = new BeanPropertyBindingResult(customerDTO, "customerDto");
         validator.validate(customerDTO, errors);

         if(errors.hasErrors()) {
             throw new ServerWebInputException(errors.toString());
         }
    }

    public Mono<ServerResponse> listCustomers(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .body(customerService.listCustomers(), CustomerDTO.class);
    }

    public Mono<ServerResponse> getCustomerById(ServerRequest request) {
        return ServerResponse.ok()
                .body(customerService.getById(request.pathVariable("customerId"))
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND))),
                        CustomerDTO.class);
    }

    // call flatMap because it is returning a new Publisher .. avoid the additional wrapping/nested, eg Mono<Mono<R>>
    //   .map() would produce itself a Mono<R>
    public Mono<ServerResponse> createNewCustomer(ServerRequest request) {
        return customerService.saveNewCustomer(request.bodyToMono(CustomerDTO.class).doOnNext(this::validate))
                .flatMap(customerDTO -> ServerResponse
                                .created(UriComponentsBuilder
                                        .fromPath(CustomerRouterConfig.CUSTOMER_PATH_ID)
                                        .build(customerDTO.getId()))
                                .build());
    }

    public Mono<ServerResponse> updateCustomerById(ServerRequest request) {
        return request.bodyToMono(CustomerDTO.class)
                .doOnNext(this::validate)
                .flatMap(customerDTO ->
                        customerService.updateCustomer(request.pathVariable("customerId"), customerDTO))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(savedDto -> ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> patchCustomerById(ServerRequest request) {
        return request.bodyToMono(CustomerDTO.class)
                .doOnNext(this::validate)
                .flatMap(customerDTO ->
                        customerService.patchCustomer(request.pathVariable("customerId"), customerDTO)
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(savedDto -> ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> deleteCustomerById(ServerRequest request) {
        return customerService.getById(request.pathVariable("customerId"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(dto -> customerService.deleteCustomerById(request.pathVariable("customerId")))
                    .then(ServerResponse.noContent().build());
    }
}

package guru.springframework.reactivemongo.services;

import guru.springframework.reactivemongo.domain.Customer;
import guru.springframework.reactivemongo.mappers.ICustomerMapper;
import guru.springframework.reactivemongo.mappers.ICustomerMapperImpl;
import guru.springframework.reactivemongo.model.CustomerDTO;
import guru.springframework.reactivemongo.repositories.ICustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest

public class CustomerServiceTest {
    @Autowired
    ICustomerService customerService;

    @Autowired
    ICustomerMapper customerMapper;

    @Autowired
    ICustomerRepository customerRepository;

    CustomerDTO customerDTO;

    @BeforeEach
    void setup(){
        customerDTO = customerMapper.customerToCustomerDto(getTestCustomer());
    }

    @Test
    @DisplayName("Test Save Customer Using Subscriber")
    void testSaveCustomerUseSubscriber() {

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        AtomicReference<CustomerDTO> atomicDto = new AtomicReference<>();

        Mono<CustomerDTO> savedMono = customerService.saveNewCustomer(Mono.just(customerDTO));

        savedMono.subscribe(savedDto -> {
            System.out.println(savedDto.getId());
            atomicBoolean.set(true);
            atomicDto.set(savedDto);
        });

        await().untilTrue(atomicBoolean);

        CustomerDTO persistedDto = atomicDto.get();
        assertThat(persistedDto).isNotNull();
        assertThat(persistedDto.getId()).isNotNull();
    }

    @Test
    @DisplayName("Test Save Customer Using Block")
    void testSaveCustomerUseBlock() {
        CustomerDTO savedDto = customerService.saveNewCustomer(Mono.just(getTestCustomerDto())).block();
        assertThat(savedDto).isNotNull();
        assertThat(savedDto.getId()).isNotNull();
    }

    @Test
    @DisplayName("Test Update Customer Using Block")
    void testUpdateBlocking() {
        final String newName = "New Customer Name";  // use final so cannot mutate
        CustomerDTO savedDto = getSavedCustomerDto();
        savedDto.setCustomerName(newName);

        CustomerDTO updatedDto = customerService.saveNewCustomer(Mono.just(savedDto)).block();

        //verify exists in db
        CustomerDTO fetchedDto = customerService.getById(updatedDto.getId()).block();
        assertThat(fetchedDto.getCustomerName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("Test Update Using Reactive Streams")
    void testUpdateStreaming() {
        final String newName = "New Customer Name";  // use final so cannot mutate

        AtomicReference<CustomerDTO> atomicDto = new AtomicReference<>();

        customerService.saveNewCustomer(Mono.just(getTestCustomerDto()))
                .map(savedCustomerDto -> {
                    savedCustomerDto.setCustomerName(newName);
                    return savedCustomerDto;
                })
                .flatMap(customerService::saveNewCustomer) // save updated customer
                .flatMap(savedUpdatedDto -> customerService.getById(savedUpdatedDto.getId())) // get from db
                .subscribe(dtoFromDb -> {
                    atomicDto.set(dtoFromDb);
                });

        await().until(() -> atomicDto.get() != null);
        assertThat(atomicDto.get().getCustomerName()).isEqualTo(newName);
    }

    @Test
    void testDeleteCustomer() {
        CustomerDTO customerToDelete = getSavedCustomerDto();

        customerService.deleteCustomerById(customerToDelete.getId()).block();

        Mono<CustomerDTO> expectedEmptyCustomerMono = customerService.getById(customerToDelete.getId());

        CustomerDTO emptyCustomer = expectedEmptyCustomerMono.block();

        assertThat(emptyCustomer).isNull();
    }

    @Test
    void testFindFirstByCustomerName() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        CustomerDTO customerDto = getSavedCustomerDto();

        Mono<CustomerDTO> foundDto = customerService.findFirstByCustomerName(customerDto.getCustomerName());
        foundDto.subscribe(dto -> {
            System.out.println(dto.toString());
            atomicBoolean.set(true);

        });

        await().untilTrue(atomicBoolean);
    }

    public CustomerDTO getSavedCustomerDto(){
        return customerService.saveNewCustomer(Mono.just(getTestCustomerDto())).block();
    }

    public static CustomerDTO getTestCustomerDto(){
        return new ICustomerMapperImpl().customerToCustomerDto(getTestCustomer());
    }

    public static Customer getTestCustomer() {
        return Customer.builder()
                .customerName("Customer1")
                .build();
    }
}
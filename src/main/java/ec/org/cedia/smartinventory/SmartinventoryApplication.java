package ec.org.cedia.smartinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ec.org.cedia.smartinventory.model.Product;

@SpringBootApplication
public class SmartinventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartinventoryApplication.class, args);
		    // Solo para demostrar Lombok — borrar después
		Product p = Product.builder()
			.id(1L)
			.name("Laptop")
			.price(1200.0)
			.stock(10)
			.active(true)
			.build();

		System.out.println(p); // toString() de @Data
	}

}

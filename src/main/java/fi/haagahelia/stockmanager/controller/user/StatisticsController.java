package fi.haagahelia.stockmanager.controller.user;

import fi.haagahelia.stockmanager.dto.common.ErrorResponse;
import fi.haagahelia.stockmanager.dto.common.OrderStatisticsDTO;
import fi.haagahelia.stockmanager.dto.common.OrderStatisticsEmployeeDTO;
import fi.haagahelia.stockmanager.dto.common.StatisticBasicResultDTO;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrder;
import fi.haagahelia.stockmanager.model.customer.order.CustomerOrderLine;
import fi.haagahelia.stockmanager.model.product.Product;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrder;
import fi.haagahelia.stockmanager.model.supplier.order.SupplierOrderLine;
import fi.haagahelia.stockmanager.model.user.Employee;
import fi.haagahelia.stockmanager.repository.customer.order.CustomerOrderRepository;
import fi.haagahelia.stockmanager.repository.product.ProductRepository;
import fi.haagahelia.stockmanager.repository.supplier.order.SupplierOrderRepository;
import fi.haagahelia.stockmanager.repository.user.EmployeeRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Log4j2
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    /* ----------------------------------------- REPOSITORIES & CONSTRUCTOR ----------------------------------------- */

    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private final CustomerOrderRepository customerOrderRepository;

    @Autowired
    public StatisticsController(EmployeeRepository employeeRepository, ProductRepository productRepository,
                                SupplierOrderRepository supplierOrderRepository, CustomerOrderRepository customerOrderRepository) {
        this.employeeRepository = employeeRepository;
        this.productRepository = productRepository;
        this.supplierOrderRepository = supplierOrderRepository;
        this.customerOrderRepository = customerOrderRepository;
    }


    /* ------------------------------------------------- API METHODS ------------------------------------------------ */


    /**
     * Available for: ROLE_MANAGER & ROLE_ADMIN
     * This function is used to calculate three values about the supplier orders:
     *      - The total of supplier orders.
     *      - The total of supplier orders for the selected month and year.
     *      - The value of the total of the supplier orders for the selected month and year.
     *
     * @param user Corresponds to the authenticated user.
     * @param date Corresponds to the date that the user wants the statistics.
     * @return A ResponseEntity containing the calculated statistics or an error message.
     *      --> HttpStatus.OK if the statistics has been calculated correctly. (Statistics)
     *      --> HttpStatus.NO_CONTENT if no supplier orders have been found. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/suppliers", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> suppliersStats(@AuthenticationPrincipal Employee user, @RequestParam(name = "date", required = false) LocalDate date) {
        try {
            log.info("User {} is requesting to get general suppliers statistics.", user.getUsername());
            if (date == null) date = LocalDate.now();
            OrderStatisticsDTO orderStatisticsDTO = new OrderStatisticsDTO();
            orderStatisticsDTO.setDate(date);

            List<SupplierOrder> supplierOrders = supplierOrderRepository.findAll();
            if (supplierOrders.size() < 1) {
                log.info("User {} requested to get general suppliers statistics. NO SUPPLIER ORDERS.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_SUPPLIER_ORDERS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }

            // Calculation of the different values
            orderStatisticsDTO.setTotalOrders(supplierOrders.size());
            for (SupplierOrder order: supplierOrders) {
                if (order.getDate().getMonth().equals(date.getMonth()) && order.getDate().getYear() == date.getYear()) {
                    List<SupplierOrderLine> supplierOrderLines = order.getSupplierOrderLines();
                    for (SupplierOrderLine supplierOrderLine : supplierOrderLines) {
                        orderStatisticsDTO.addOrderValueForTheMonth(supplierOrderLine.getQuantity() * supplierOrderLine.getBuyPrice());
                    }
                    orderStatisticsDTO.incrTotalOrderForMonth();
                }
            }
            Link selfRel = linkTo(StatisticsController.class).slash("suppliers").withSelfRel();
            orderStatisticsDTO.add(selfRel);
            return new ResponseEntity<>(orderStatisticsDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to get general suppliers statistics. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER & ROLE_ADMIN
     * This function is used to calculate three values about the customer orders:
     *      - The total of customer orders.
     *      - The total of customer orders for the selected month and year.
     *      - The value of the total of the customer orders for the selected month and year.
     *
     * @param user Corresponds to the authenticated user.
     * @param date Corresponds to the date that the user wants the statistics.
     * @return A ResponseEntity containing the calculated statistics or an error message.
     *      --> HttpStatus.OK if the statistics has been calculated correctly. (Statistics)
     *      --> HttpStatus.NO_CONTENT if no customer orders have been found. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/customers", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> customersStats(@AuthenticationPrincipal Employee user, @RequestParam(name = "date", required = false) LocalDate date) {
        try {
            log.info("User {} is requesting to get general customer statistics.", user.getUsername());
            if (date == null) date = LocalDate.now();
            OrderStatisticsDTO orderStatisticsDTO = new OrderStatisticsDTO();
            orderStatisticsDTO.setDate(date);

            List<CustomerOrder> customerOrders = customerOrderRepository.findAll();
            if (customerOrders.size() < 1) {
                log.info("User {} requested to get general customer statistics. NO CUSTOMER ORDERS.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMERS_ORDERS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }

            // Calculation of the different values
            orderStatisticsDTO.setTotalOrders(customerOrders.size());
            for (CustomerOrder order: customerOrders) {
                if (order.getDate().getMonth().equals(date.getMonth()) && order.getDate().getYear() == date.getYear()) {
                    List<CustomerOrderLine> customerOrderLines = order.getCustomerOrderLines();
                    for (CustomerOrderLine customerOrderLine : customerOrderLines) {
                        orderStatisticsDTO.addOrderValueForTheMonth(customerOrderLine.getQuantity() * customerOrderLine.getSellPrice());
                    }
                    orderStatisticsDTO.incrTotalOrderForMonth();
                }
            }
            Link selfRel = linkTo(StatisticsController.class).slash("customers").withSelfRel();
            orderStatisticsDTO.add(selfRel);
            return new ResponseEntity<>(orderStatisticsDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to get general customer statistics. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER & ROLE_ADMIN
     * This function is used to calculate statistics for an employee on a defined month and year.
     * Firstly, we check that an employee corresponds to the given id.
     *      --> If not, we return an HttpStatus.BAD_REQUEST.
     * Secondly, we select all the customer orders that are in the database
     *      --> If there is no customer orders, we return an HttpStatus.NO_CONTENT.
     * Thirdly, we sort and calculate the data based on the returned list of the previous step.
     * Finally, we return statistics to the user.
     *
     * @param empId Corresponds to the id of the employee from whom we want the statistics
     * @param user Corresponds to the authenticated user.
     * @param date Corresponds to the date that the user wants the statistics.
     * @return A ResponseEntity containing the calculated statistics or an error message.
     *      --> HttpStatus.OK if the statistics has been calculated correctly. (Statistics)
     *      --> HttpStatus.BAD_REQUEST if no employee corresponds to the given id. (ErrorMessage)
     *      --> HttpStatus.NO_CONTENT if no customer orders have been found. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/employee={empId}")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> salesPerVendor(@PathVariable(value = "empId") Long empId, @AuthenticationPrincipal Employee user,
                                            @RequestParam(name = "date", required = false) LocalDate date) {
        try {
            log.info("User {} is requesting to get statistics for the vendor: {}.", user.getUsername(), empId);
            if (date == null) date = LocalDate.now();

            Optional<Employee> employeeOptional = employeeRepository.findById(empId);
            if (employeeOptional.isEmpty()) {
                log.info("User {} requested to get statistics for the vendor: {}. NO EMPLOYEE FOUND.", user.getUsername(), empId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), "NO_EMPLOYEE_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.BAD_REQUEST);
            }
            Employee employee = employeeOptional.get();

            List<CustomerOrder> customerOrders = customerOrderRepository.findAll();
            if (customerOrders.size() < 1) {
                log.info("User {} requested to get statistics for the vendor: {}. NO CUSTOMER ORDERS.", user.getUsername(), empId);
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDERS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }

            OrderStatisticsEmployeeDTO orderStatisticsEmployeeDTO = new OrderStatisticsEmployeeDTO();
            orderStatisticsEmployeeDTO.setDate(date);
            orderStatisticsEmployeeDTO.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());

            for (CustomerOrder customerOrder : customerOrders) {
                if (customerOrder.getEmployee().getId().equals(empId)) {
                    orderStatisticsEmployeeDTO.incrTotalOrders();
                    if (customerOrder.getDate().getMonth().equals(date.getMonth()) && customerOrder.getDate().getYear() == date.getYear()) {
                        orderStatisticsEmployeeDTO.incrTotalOrderForMonth();
                        for (CustomerOrderLine customerOrderLine : customerOrder.getCustomerOrderLines()) {
                            orderStatisticsEmployeeDTO.addOrderValueForTheMonth(customerOrderLine.getQuantity() * customerOrderLine.getSellPrice());
                        }
                    }
                }
            }
            Link selfRel = linkTo(StatisticsController.class).slash("/employee=" + empId).withSelfRel();
            Link employeeLink = linkTo(EmployeeController.class).slash(empId).withRel("employee");
            orderStatisticsEmployeeDTO.add(selfRel, employeeLink);
            return new ResponseEntity<>(orderStatisticsEmployeeDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to get statistics for the vendor: {}. UNEXPECTED ERROR!", user.getUsername(), empId);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER & ROLE_ADMIN
     * This function is used to calculate the stock to sales ratio. Formula: inventory value / sales value
     * Firstly, we find all the products that are in the database.
     *      --> If there is no product, we return an HttpStatus.NO_CONTENT.
     * Secondly, we calculate the total inventory value (stock of each product multiplied by its pruchase price)
     * Thirdly, we search all the customer orders that are in the database.
     *      --> If there is no customer order, we return an HttpStatus.NO_CONTENT.
     * Fourthly, we calculate the values of all the sales of the searched date (month and year).
     * Finally, we return the calculated statistics to the user.
     *
     * @param user Corresponds to the authenticated user.
     * @param date Corresponds to the date that the user wants the statistics.
     * @return A ResponseEntity containing the calculated statistics or an error message.
     *      --> HttpStatus.OK if the statistics has been calculated correctly. (Statistics)
     *      --> HttpStatus.NO_CONTENT if there is no product or no customer orders. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/stock-to-sale-ratio", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> stockToSalesRatio(@AuthenticationPrincipal Employee user, @RequestParam(name = "date", required = false) LocalDate date) {
        try {
            log.info("User {} is requesting to get the stock to sale ratio.", user.getUsername());
            if (date == null) date = LocalDate.now();

            // Calculation of the value of the inventory.
            double inventoryValue = 0.0;
            List<Product> products = productRepository.findAll();
            if (products.size() < 1) {
                log.info("User {} requested to get the stock to sale ratio. NO PRODUCT FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_PRODUCTS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            for (Product p : products) {
                inventoryValue += p.getStock() * p.getPurchasePrice();
            }

            // Calculation of the value of all the customer orders that corresponds to the date.
            double salesValue = 0.0;
            List<CustomerOrder> allCustomerOrders = customerOrderRepository.findAll();
            if (allCustomerOrders.size() < 1) {
                log.info("User {} requested to get the stock to sale ratio. NO CUSTOMER ORDERS.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDERS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            for (CustomerOrder order : allCustomerOrders) {
                if (order.getDate().getMonth().equals(date.getMonth()) && order.getDate().getYear() == date.getYear()) {
                    for (CustomerOrderLine orderLine : order.getCustomerOrderLines()) {
                        salesValue += orderLine.getQuantity() * orderLine.getSellPrice();
                    }
                }
            }

            // Ratio calculation
            StatisticBasicResultDTO<Double> resultDTO = new StatisticBasicResultDTO<>();
            System.out.println("Inventory value" + inventoryValue);
            System.out.println("Sales value" + salesValue);
            System.out.println("Calculation" + inventoryValue / salesValue);
            resultDTO.setResultName("Stock to sale ratio"); resultDTO.setResultValue(inventoryValue / salesValue);
            Link selfRel = linkTo(StatisticsController.class).slash("stock-to-sale-ratio").withSelfRel();
            resultDTO.add(selfRel);
            return new ResponseEntity<>(resultDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to get the stock to sale ratio. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER & ROLE_ADMIN
     * This function is used to calculate the sell-through rate. Formula: (units sold / units received) x 100
     * Firstly, we select all the customer orders from the database.
     *      --> If there is no customer order, we return an HttpStatus.NO_CONTENT.
     * Secondly, we calculate the number of unit that has been sold for the corresponding date (month & year).
     * Thirdly, we find all the supplier orders from the database.
     *      --> If there is no supplier order, we return an HttpStatus.NO_CONTENT.
     * Fourthly, we calculate the number of unit that has been received for the corresponding date (month & year).
     * Finally, we calculate the rate using the two previous measures, and we return it with an HttpStatus.OK.
     *
     * @param user Corresponds to the authenticated user.
     * @param date Corresponds to the date that the user wants the statistics.
     * @return A ResponseEntity containing the calculated statistics or an error message.
     *      --> HttpStatus.OK if the statistics has been calculated correctly. (Statistics)
     *      --> HttpStatus.NO_CONTENT if there is no customer order or no supplier order. (ErrorMessage)
     *      --> HttpStatus.PRECONDITION_FAILED if there is no product that has been received. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/sell-through-rate", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> SellThroughRate(@AuthenticationPrincipal Employee user, @RequestParam(name = "date", required = false) LocalDate date) {
        try {
            log.info("User {} is requesting to get the sell-through rate.", user.getUsername());
            if (date == null) date = LocalDate.now();

            // Calculation of number of unit sold
            int unitSold = 0;
            List<CustomerOrder> allCustomerOrders = customerOrderRepository.findAll();
            if (allCustomerOrders.size() < 1) {
                log.info("User {} requested to get the sell-through rate. NO CUSTOMER ORDERS.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDERS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            for (CustomerOrder order : allCustomerOrders) {
                if (order.getDate().getMonth().equals(date.getMonth()) && order.getDate().getYear() == date.getYear()) {
                    for (CustomerOrderLine orderLine : order.getCustomerOrderLines()) {
                        unitSold += orderLine.getQuantity();
                    }
                }
            }

            // Calculation of number of unit received
            int unitReceived = 0;
            List<SupplierOrder> allSupplierOrders = supplierOrderRepository.findAll();
            if (allSupplierOrders.size() < 1) {
                log.info("User {} requested to get the sell-through rate. NO SUPPLIER ORDERS.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_SUPPLIER_ORDERS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            for (SupplierOrder suppOrder : allSupplierOrders) {
                if (suppOrder.getReceived()) {
                    if (suppOrder.getDate().getMonth().equals(date.getMonth()) && suppOrder.getDate().getYear() == date.getYear()) {
                        for (SupplierOrderLine supOrderLine : suppOrder.getSupplierOrderLines()) {
                            unitReceived += supOrderLine.getQuantity();
                        }
                    }
                }
            }
            if (unitReceived == 0) {
                log.info("User {} requested to get the sell-through rate. NO PRODUCT RECEIVED.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "NO_PRODUCT_RECEIVED");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }

            // Percentage calculation
            StatisticBasicResultDTO<Integer> resultDTO = new StatisticBasicResultDTO<>();
            resultDTO.setResultName("Sell-through rate"); resultDTO.setResultValue((unitSold / unitReceived) * 100);
            Link selfRel = linkTo(StatisticsController.class).slash("sell-through-rate").withSelfRel();
            resultDTO.add(selfRel);
            return new ResponseEntity<>(resultDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to get the sell-through rate. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER & ROLE_ADMIN
     * This function is used to calculate the stock-outs percentage. Formula: (items out of stock / items available) x 100
     * Firstly, we find all the products that are in the database.
     *      --> If there is no product, we return an HttpStatus.NO_CONTENT.
     * Secondly, we calculate all the available products and all the outOfStock products.
     *      --> If the total of available products is equals to 0, we return an HttpStatus.PRECONDITION_FAILED.
     * Finally, we calculate the rate using the two previous variables, and we return it with an HttpStatus.OK.
     *
     * @param user Corresponds to the authenticated user.
     * @return A ResponseEntity containing the calculated statistics or an error message.
     *      --> HttpStatus.OK if the statistics has been calculated correctly. (Statistics)
     *      --> HttpStatus.NO_CONTENT if there is no product. (ErrorMessage)
     *      --> HttpStatus.PRECONDITION_FAILED if there is no product available. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/stock-outs", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> stockOuts(@AuthenticationPrincipal Employee user) {
        try {
            log.info("User {} is requesting to get the stock-outs.", user.getUsername());
            int outOfStock = 0;
            int availableProducts = 0;
            List<Product> products = productRepository.findAll();
            if (products.size() < 1) {
                log.info("User {} requested to get the stock-outs. NO PRODUCT FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_PRODUCTS_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }

            // Calculation of available and out of stock products
            for (Product product : products) {
                if (product.getStock() != null && product.getStock() > 0) {
                    availableProducts += 1;
                } else {
                    outOfStock += 1;
                }
            }
            if (availableProducts == 0) {
                log.info("User {} requested to get the stock-outs. NO AVAILABLE PRODUCTS.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "NO_AVAILABLE_PRODUCTS");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }

            // Percentage calculation
            StatisticBasicResultDTO<Integer> resultDTO = new StatisticBasicResultDTO<>();
            resultDTO.setResultName("Stock-outs"); resultDTO.setResultValue((outOfStock / availableProducts) * 100);
            Link selfRel = linkTo(StatisticsController.class).slash("stock-outs").withSelfRel();
            resultDTO.add(selfRel);
            return new ResponseEntity<>(resultDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to get the stock-outs. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Available for: ROLE_MANAGER & ROLE_ADMIN
     * This function is used to calculate the service level percentage. Formula: (orders delivered / orders received) x 100
     * Firstly, we select all the customer orders from the database.
     *      --> If there is no customer order, we return an HttpStatus.NO_CONTENT
     * Secondly, we calculate the number of orders that have been delivered to the customers.
     * Thirdly, we find all the supplier orders from the database.
     *      --> If there is no supplier order, we return an HttpStatus.NO_CONTENT
     * Fourthly, we calculate the number of orders that have been received from the suppliers.
     * Fifthly, we check that the number of received order is not equals to 0.
     *      --> If it is the case, we return an HttpStatus.PRECONDITION_FAILED.
     * Finally, we calculate the percentage, and we return it with an HttpStatus.OK.
     *
     * @param user Corresponds to the authenticated user.
     * @param date Corresponds to the date that the user wants the statistics.
     * @return A ResponseEntity containing the calculated statistics or an error message.
     *      --> HttpStatus.OK if the statistics has been calculated correctly. (Statistics)
     *      --> HttpStatus.NO_CONTENT if there is no customer order or no supplier order. (ErrorMessage)
     *      --> HttpStatus.INTERNAL_SERVER_ERROR if another error occurs. (ErrorMessage)
     */
    @GetMapping(value = "/service-level", produces = "application/json")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> serviceLevel(@AuthenticationPrincipal Employee user, @RequestParam(name = "date", required = false ) LocalDate date) {
        try {
            log.info("User {} is requesting to get the service-level percentage.", user.getUsername());
            if (date == null) date = LocalDate.now();

            // Calculation of the number of orders shipped
            int ordersDelivered = 0;
            List<CustomerOrder> customerOrders = customerOrderRepository.findAll();
            if (customerOrders.size() < 1) {
                log.info("User {} requested to get the service-level percentage. NO CUSTOMER ORDER FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_CUSTOMER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            for (CustomerOrder customerOrder : customerOrders) {
                if (customerOrder.getDate().getMonth().equals(date.getMonth()) && customerOrder.getDate().getYear() == date.getYear()) {
                    if (customerOrder.getSent()) {
                        ordersDelivered += 1;
                    }
                }
            }

            // Calculation of the number of orders received
            int ordersReceived = 0;
            List<SupplierOrder> supplierOrders = supplierOrderRepository.findAll();
            if (supplierOrders.size() < 1) {
                log.info("User {} requested to get the service-level percentage. NO SUPPLIER ORDER FOUND.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.NO_CONTENT.getReasonPhrase(), "NO_SUPPLIER_ORDER_FOUND");
                return new ResponseEntity<>(bm, HttpStatus.NO_CONTENT);
            }
            for (SupplierOrder supplierOrder : supplierOrders) {
                if (supplierOrder.getDate().getMonth().equals(date.getMonth()) && supplierOrder.getDate().getYear() == date.getYear()) {
                    if (supplierOrder.getReceived()) {
                        ordersReceived += 1;
                    }
                }
            }
            if (ordersReceived == 0) {
                log.info("User {} requested to get the service-level percentage. NO SUPPLIER ORDER RECEIVED.", user.getUsername());
                ErrorResponse bm = new ErrorResponse(HttpStatus.PRECONDITION_FAILED.getReasonPhrase(), "NO_ORDER_RECEIVED");
                return new ResponseEntity<>(bm, HttpStatus.PRECONDITION_FAILED);
            }

            // Percentage calculation
            StatisticBasicResultDTO<Integer> resultDTO = new StatisticBasicResultDTO<>();
            resultDTO.setResultName("Service-level"); resultDTO.setResultValue((ordersDelivered / ordersReceived) * 100);
            Link selfRel = linkTo(StatisticsController.class).slash("service-level").withSelfRel();
            resultDTO.add(selfRel);
            return new ResponseEntity<>(resultDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.info("User {} requested to get the service-level percentage. UNEXPECTED ERROR!", user.getUsername());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

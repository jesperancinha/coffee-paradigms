package com.jesperancinha.coffee.system.manager;

import org.jesperancinha.coffee.system.input.CoffeeMachines;
import org.jesperancinha.coffee.system.input.CoffeeMachines.CoffeMachine.Coffees.Coffee;
import org.jesperancinha.coffee.system.input.CoffeeMachines.CoffeMachine.PaymentTypes.Payment;
import org.jesperancinha.coffee.system.input.Employees;
import org.jesperancinha.coffee.system.input.Employees.Employee;
import com.jesperancinha.coffee.system.objects.EmployeeLayer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Service
@Slf4j
public class GeneralProcessorImpl {

    public static final String MAIN_QUEUE_PRE = "MAIN_QUEUE_PRE";

    public static final String MAIN_QUEUE_POST = "MAIN_QUEUE_POST";

    private int nIterations;
    private String sourceXmlMachinesFile;
    private String sourceXmlEmployeesFile;
    private int preRowSize;
    private int postRowSize;

    private CoffeeMachines coffeeMachines;
    private Employees employees;

    @Autowired
    private MachineProcessorImpl machineProcessor;

    /**
     * Creates the coffee machines from the XML file
     *
     * @param sourceXmlCoffeeMachineFile
     * @return CoffeeMachines list
     */
    private CoffeeMachines createCoffeeMachines(final InputStream sourceXmlCoffeeMachineFile) throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(CoffeeMachines.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (CoffeeMachines) unmarshaller.unmarshal(sourceXmlCoffeeMachineFile);
    }

    /**
     * Creates the employess from the XML file
     *
     * @param sourceXmlEmployeesFile
     * @return Employees list
     */
    private Employees createEmployees(final InputStream sourceXmlEmployeesFile)
            throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Employees.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (Employees) unmarshaller.unmarshal(sourceXmlEmployeesFile);

    }

    /**
     * Starts the simulation
     */
    public void initSimulationProcess()
            throws FileNotFoundException, JAXBException {
        log.info("Starting coffee simulation process...");
        initSimulationProcess(
                new FileInputStream(
                        this.sourceXmlMachinesFile
                ),
                new FileInputStream(
                        this.sourceXmlEmployeesFile
                )
        );
    }

    /**
     * Starts simulation using inputStreams
     *
     * @param coffeesFile
     * @param employeesFile
     * @throws FileNotFoundException
     * @throws JAXBException
     * @throws SAXException
     */
    public void initSimulationProcess(InputStream coffeesFile, InputStream employeesFile) throws JAXBException {
        log.info("Starting simulation process...");
        this.coffeeMachines = createCoffeeMachines(coffeesFile);
        this.employees = createEmployees(employeesFile);
    }

    public void start() {
        final PreProcessorImpl preProcessor = machineProcessor.getPreProcessor();
        preProcessor.addQueueSize(preRowSize, MAIN_QUEUE_PRE);
        preProcessor.initExecutors();

        final PostProcessorImpl postProcessor = machineProcessor.getPostProcessor();
        postProcessor.addQueueSize(postRowSize, MAIN_QUEUE_POST);
        postProcessor.initExecutors();

        final int nMachines = coffeeMachines.getCoffeMachine().size();
        final List<EmployeeLayer> employeeLayerList = new ArrayList<>();
        final Random random = new Random();

        fillEmployeeLayerList(nMachines, employeeLayerList, random);
        startCoffeeMeeting(employeeLayerList);

        runaAllProcessors();
        waitForAllProcessors();
        stopAllProcessors();
    }

    private void fillEmployeeLayerList(int nMachines, List<EmployeeLayer> employeeLayerList, Random random) {
        final CoffeeProcessorImpl coffeeProcessor = machineProcessor.getCoffeeProcessor();
        final PaymentProcessorImpl paymentProcessor = machineProcessor.getPaymentProcessor();
        this.employees.getEmployee().forEach(
                employee -> {
                    final int iChosenCoffeeMachine = random.nextInt(nMachines);
                    final CoffeeMachines.CoffeMachine coffeMachine = coffeeMachines.getCoffeMachine()
                            .get(iChosenCoffeeMachine);
                    final int nCoffees = coffeMachine.getCoffees().getCoffee().size();
                    final int iCoffee = random.nextInt(nCoffees);
                    final int nPayments = coffeMachine.getPaymentTypes().getPayment().size();
                    final int iPayment = random.nextInt(nPayments);
                    final Coffee chosenCoffee = coffeMachine.getCoffees().getCoffee().get(iCoffee);
                    final Payment chosenPayment = coffeMachine.getPaymentTypes().getPayment().get(iPayment);

                    coffeeProcessor.addQueueSize(1, chosenCoffee.getName());
                    paymentProcessor.addQueueSize(1, chosenPayment.getName());

                    final EmployeeLayer employeeLayer = EmployeeLayer
                            .builder()
                            .employee(employee)
                            .coffee(chosenCoffee)
                            .payment(chosenPayment)
                            .build();
                    employeeLayerList.add(employeeLayer);
                }
        );
        coffeeProcessor.initExecutors();
        paymentProcessor.initExecutors();
    }

    private void startCoffeeMeeting(List<EmployeeLayer> employeeLayerList) {
        employeeLayerList.forEach(
                employeeLayer -> {
                    final Employee employee = employeeLayer.getEmployee();
                    final List<Employee.Actions.PreAction> preActions = employee.getActions().getPreAction();
                    final Coffee coffee = employeeLayer.getCoffee();
                    final Payment payment = employeeLayer.getPayment();
                    final List<Employee.Actions.PostAction> postActions = employee.getActions().getPostAction();
                    machineProcessor.callPreActions(employee, MAIN_QUEUE_PRE, preActions, coffee, payment, postActions);

                }
        );
    }

    private void runaAllProcessors() {
        machineProcessor.getPreProcessor().runAllCalls();
        // machineProcessor.getCoffeeProcessor().runAllCalls();
        // machineProcessor.getPaymentProcessor().runAllCalls();
        // machineProcessor.getPostProcessor().runAllCalls();
    }

    private void waitForAllProcessors() {
        machineProcessor.getPreProcessor().waitForAllCalls();
        // machineProcessor.getCoffeeProcessor().waitForAllCalls();
        // machineProcessor.getPaymentProcessor().waitForAllCalls();
        // machineProcessor.getPostProcessor().waitForAllCalls();
    }

    private void stopAllProcessors() {
        machineProcessor.getPreProcessor().stopExectutors();
        //  machineProcessor.getCoffeÒØeProcessor().stopExectutors();
        //  machineProcessor.getPaymentProcessor().stopExectutors();
        //  machineProcessor.getPostProcessor().stopExectutors();
    }
}
package org.jesperancinha.coffee.system.manager

import lombok.Getter
import lombok.experimental.Accessors
import org.jesperancinha.coffee.system.api.concurrency.QueueCallable
import org.jesperancinha.coffee.system.input.CoffeeMachines.CoffeMachine.Coffees.Coffee
import org.jesperancinha.coffee.system.input.CoffeeMachines.CoffeMachine.PaymentTypes.Payment
import org.jesperancinha.coffee.system.input.Employees.Employee
import org.jesperancinha.coffee.system.input.Employees.Employee.Actions.PostAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Accessors(chain = true)
@Getter
@Service
class MachineProcessorImpl {
    @Autowired
    val preProcessor: PreProcessorImpl? = null

    @Autowired
    val coffeeProcessor: CoffeeProcessorImpl? = null

    @Autowired
    val paymentProcessor: PaymentProcessorImpl? = null

    @Autowired
    private val postProcessor: PostProcessorImpl? = null
    fun callPreActions(
        employee: Employee,
        name: String?,
        preActions: List<Employee.Actions.PreAction?>,
        coffee: Coffee,
        payment: Payment,
        postActions: List<PostAction?>
    ) {
        preProcessor!!.callPreActions(employee, name, preActions, coffee, payment, postActions)
    }

    fun callMakeCoffee(
        employee: Employee?,
        name: String,
        coffee: Coffee,
        payment: Payment,
        postActions: List<PostAction?>,
        parentCallable: QueueCallable
    ) {
        coffeeProcessor!!.callMakeCoffee(employee, name, coffee, payment, postActions, parentCallable)
    }

    fun callPayCoffee(
        employee: Employee?,
        name: String?,
        payment: Payment,
        postActions: List<PostAction?>,
        parentCallable: QueueCallable
    ) {
        paymentProcessor!!.callPayCoffee(employee, name, payment, postActions, parentCallable)
    }

    fun callPostActions(
        employee: Employee?,
        name: String?,
        postActions: List<PostAction?>,
        parentCallable: QueueCallable
    ) {
        postProcessor!!.callPostActions(employee, name, postActions, parentCallable)
    }

    fun initAll() {
        preProcessor!!.initExecutors()
        coffeeProcessor!!.initExecutors()
        paymentProcessor!!.initExecutors()
        postProcessor!!.initExecutors()
    }
}
package cl.education.enrollment.controller;

import cl.education.enrollment.dto.QueueBindingRequest;
import cl.education.enrollment.service.GuideQueueAdminService;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos para RabbitMQ: creacion/eliminacion de colas, exchanges y
 * bindings via AmqpAdmin, y control del listener del consumidor de guias de despacho
 * (pausar/reanudar/consultar estado) via RabbitListenerEndpointRegistry.
 *
 * Requiere el rol de administracion (GUIDES_MANAGE) igual que el resto de endpoints de
 * gestion de guias.
 */
@RestController
@RequestMapping("/api/rabbit-admin")
public class GuideQueueAdminController {

    private static final String CONSUMER_LISTENER_ID = "listener-dispatchGuideQueue";

    private final GuideQueueAdminService adminService;
    private final RabbitListenerEndpointRegistry listenerRegistry;

    public GuideQueueAdminController(
            GuideQueueAdminService adminService,
            RabbitListenerEndpointRegistry listenerRegistry
    ) {
        this.adminService = adminService;
        this.listenerRegistry = listenerRegistry;
    }

    @PostMapping("/colas/{nombreCola}")
    public String crearCola(@PathVariable String nombreCola) {
        adminService.crearCola(nombreCola);
        return "Cola creada: " + nombreCola;
    }

    @PostMapping("/exchanges/{nombreExchange}")
    public String crearExchange(@PathVariable String nombreExchange) {
        adminService.crearExchange(nombreExchange);
        return "Exchange creado: " + nombreExchange;
    }

    @PostMapping("/bindings")
    public String crearBinding(@RequestBody QueueBindingRequest request) {
        adminService.crearBinding(request);
        return "Binding creado entre cola " + request.queueName() + " y exchange " + request.exchangeName();
    }

    @DeleteMapping("/colas/{nombreCola}")
    public String eliminarCola(@PathVariable String nombreCola) {
        adminService.eliminarCola(nombreCola);
        return "Cola eliminada: " + nombreCola;
    }

    @DeleteMapping("/exchanges/{nombreExchange}")
    public String eliminarExchange(@PathVariable String nombreExchange) {
        adminService.eliminarExchange(nombreExchange);
        return "Exchange eliminado: " + nombreExchange;
    }

    @PostMapping("/listener/pausar")
    public String pausarListener() {
        MessageListenerContainer container = listenerRegistry.getListenerContainer(CONSUMER_LISTENER_ID);
        if (container != null && container.isRunning()) {
            container.stop();
            return "Listener pausado: " + CONSUMER_LISTENER_ID;
        }
        return "El listener ya estaba detenido: " + CONSUMER_LISTENER_ID;
    }

    @PostMapping("/listener/reanudar")
    public String reanudarListener() {
        MessageListenerContainer container = listenerRegistry.getListenerContainer(CONSUMER_LISTENER_ID);
        if (container != null && !container.isRunning()) {
            container.start();
            return "Listener reanudado: " + CONSUMER_LISTENER_ID;
        }
        return "El listener ya estaba activo: " + CONSUMER_LISTENER_ID;
    }

    @PostMapping("/listener/status")
    public String statusListener() {
        MessageListenerContainer container = listenerRegistry.getListenerContainer(CONSUMER_LISTENER_ID);
        boolean running = container != null && container.isRunning();
        return "Listener " + CONSUMER_LISTENER_ID + " esta " + (running ? "activo" : "pausado");
    }
}

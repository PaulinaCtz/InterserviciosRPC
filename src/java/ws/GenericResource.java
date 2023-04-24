/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/WebServices/GenericResource.java to edit this template
 */
package ws;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;



/**
 * REST Web Service
 *
 * @author Paulina Cortez Alamilla.
 */
@Path("/fibonacci/{n}")
public class GenericResource {

    private static final String RPC_QUEUE_NAME = "rpc_queue";

    /**
     * Creates a new instance of GenericResource
     */
    public GenericResource() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getNumeroFibonacci(@PathParam("n") int n) {
        try {
            int result = sendRpcRequest(n);
            return "El número Fibonacci de " + n + " es: " + result;
        } catch (Exception e) {
            return "Error al obtener el número Fibonacci";
        }
    }

    private int sendRpcRequest(int n) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Especifica la dirección del servidor RabbitMQ

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            String replyQueueName = channel.queueDeclare().getQueue();
            final String corrId = java.util.UUID.randomUUID().toString();

            String message = Integer.toString(n);

            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish("", RPC_QUEUE_NAME, props, message.getBytes("UTF-8"));

            final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    if (properties.getCorrelationId().equals(corrId)) {
                        response.offer(new String(body, "UTF-8"));
                    }
                }
            };

            channel.basicConsume(replyQueueName, true, consumer);

            String result = response.take();

            return Integer.parseInt(result);
        }
    }
}
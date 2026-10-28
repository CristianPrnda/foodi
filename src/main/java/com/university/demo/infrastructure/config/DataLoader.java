package com.university.demo.infrastructure.config;

import com.university.demo.application.service.FacturaService;
import com.university.demo.application.service.PrediccionIAService;
import com.university.demo.domain.*;
import com.university.demo.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Datos de demostración para el perfil "h2" (la BD en memoria arranca vacía).
 * Usuarios de prueba (clave foodi123): jorge@ (ADMINISTRADOR), andrea@ (CAJERO), carlos@ (MESERO)
 * todos @lafondadelllano.com. Genera además 2 semanas de pedidos y facturas para que el
 * dashboard y la predicción IA tengan historial. Se puede borrar cuando exista la BD real.
 */
@Component
@Profile("h2")
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private record Item(int cantidad, String descripcion, long precio) {
    }

    private static final List<Item> CARTA = List.of(
            new Item(1, "Bandeja paisa", 32000), new Item(1, "Sancocho trifásico", 30000),
            new Item(1, "Ajiaco santafereño", 26000), new Item(1, "Limonada de coco", 9000),
            new Item(1, "Limonada natural", 8000), new Item(1, "Cerveza Club Colombia", 7000),
            new Item(1, "Jugo de mora", 8000), new Item(1, "Tres leches", 11000),
            new Item(1, "Arepa con queso", 6000), new Item(1, "Trucha al ajillo", 34000));

    private static final String[] MESEROS = {"Carlos R.", "Andrea P.", "Luisa M."};

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final RestauranteRepository restauranteRepository;
    private final ProveedorRepository proveedorRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final FacturaRepository facturaRepository;
    private final PasswordEncoder passwordEncoder;
    private final FacturaService facturaService;
    private final PrediccionIAService prediccionService;

    private int contadorPedidos = 1000;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        cargarUsuarios();
        Restaurante restaurante = cargarRestaurante();
        List<Cliente> clientes = cargarClientes();
        cargarHistorial(clientes);
        cargarMesasEnServicio(restaurante);
        marcarFacturasDeEjemplo();
        prediccionService.generarPrediccion();
    }

    // ---------- usuarios ----------

    private void cargarUsuarios() {
        Rol admin = rolRepository.save(new Rol("ADMINISTRADOR"));
        Rol cajero = rolRepository.save(new Rol("CAJERO"));
        Rol mesero = rolRepository.save(new Rol("MESERO"));
        usuario("Jorge Mejía", "jorge@lafondadelllano.com", admin);
        usuario("Andrea Patiño", "andrea@lafondadelllano.com", cajero);
        usuario("Carlos Rincón", "carlos@lafondadelllano.com", mesero);
    }

    private void usuario(String nombre, String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setContrasena(passwordEncoder.encode("foodi123"));
        u.setRol(rol);
        usuarioRepository.save(u);
    }

    // ---------- restaurante, mesas, inventario ----------

    private Restaurante cargarRestaurante() {
        Restaurante restaurante = new Restaurante();
        restaurante.setNombre("La Fonda del Llano");
        restaurante.setSede("Sede Chapinero");
        restaurante.setCiudad("Bogotá");

        for (int n = 1; n <= 16; n++) {
            Mesa mesa = new Mesa();
            mesa.setNumero(n);
            restaurante.registrarMesa(mesa);
        }

        Inventario inventario = new Inventario();
        inventario.setNombre("Inventario principal");
        inventario.setRestaurante(restaurante);
        restaurante.setInventario(inventario);

        Proveedor carnes = proveedor("Carnes del Llano", "+57 601 555 0101");
        Proveedor avicola = proveedor("Avícola San José", "+57 601 555 0102");
        Proveedor diana = proveedor("Diana SA", "+57 601 555 0103");
        Proveedor frutas = proveedor("Frutas el Trébol", "+57 601 555 0104");
        Proveedor plaza = proveedor("Plaza Paloquemao", "+57 601 555 0105");
        Proveedor bavaria = proveedor("Bavaria Distribuidor", "+57 601 555 0106");

        inventario.agregarIngrediente(ingrediente("Carne de res molida", 2.4, "kg", 8, 2500, carnes));
        inventario.agregarIngrediente(ingrediente("Pechuga de pollo", 14.8, "kg", 10, 14800, avicola));
        inventario.agregarIngrediente(ingrediente("Arroz blanco premium", 38, "kg", 25, 4200, diana));
        inventario.agregarIngrediente(ingrediente("Aguacate hass", 18, "un.", 40, 2100, frutas));
        inventario.agregarIngrediente(ingrediente("Tomate chonto", 9.2, "kg", 6, 3800, plaza));
        inventario.agregarIngrediente(ingrediente("Limón Tahití", 4.4, "kg", 5, 3200, plaza));
        inventario.agregarIngrediente(ingrediente("Cerveza Club Colombia", 24, "bot.", 60, 3500, bavaria));

        return restauranteRepository.save(restaurante);
    }

    private Proveedor proveedor(String nombre, String telefono) {
        Proveedor p = new Proveedor();
        p.setNombre(nombre);
        p.setTelefono(telefono);
        return proveedorRepository.save(p);
    }

    private Ingrediente ingrediente(String nombre, double stock, String unidad, double minimo,
                                    long costo, Proveedor proveedor) {
        Ingrediente i = new Ingrediente();
        i.setNombre(nombre);
        i.setStock(stock);
        i.setUnidad(unidad);
        i.setStockMinimo(minimo);
        i.setCostoUnitario(BigDecimal.valueOf(costo));
        i.setProveedor(proveedor);
        i.recalcularEstado();
        return i;
    }

    // ---------- clientes ----------

    private List<Cliente> cargarClientes() {
        List<Cliente> lista = new ArrayList<>();
        lista.add(cliente("Inversiones Aleph SAS", "NIT 900.234.567-1", "pagos@aleph.co", "+57 311 4567", "Cra 11 #93-12, Bogotá", "3,2 sem", true));
        lista.add(cliente("Marcela Vélez Botero", "CC 52.778.901", "marcela.v@correo.com", null, null, "1,5 sem", false));
        lista.add(cliente("Andrés Hoyos Castaño", "CC 79.111.402", "a.hoyos@gmail.com", null, null, "4 sem", false));
        lista.add(cliente("Tatiana Ruiz Mejía", "CC 1.020.450.118", "tatiruiz@outlook.com", null, null, "2 sem", false));
        lista.add(cliente("Soluciones Cocina Verde", "NIT 901.567.890-2", "admin@cocinaverde.co", null, null, "5 sem", true));
        return lista;
    }

    private Cliente cliente(String nombre, String documento, String email, String tel, String dir,
                            String frecuencia, boolean vip) {
        Cliente c = new Cliente();
        c.setDocumento(documento);
        c.actualizarDatos(nombre, email, tel, dir);
        c.setFrecuencia(frecuencia);
        c.setVip(vip);
        return clienteRepository.save(c);
    }

    // ---------- historial de ventas (14 días + hoy) ----------

    private void cargarHistorial(List<Cliente> clientes) {
        Random azar = new Random(42);
        LocalDateTime ahora = LocalDateTime.now();
        int contadorCliente = 0;

        for (int atras = 14; atras >= 0; atras--) {
            LocalDate dia = LocalDate.now().minusDays(atras);
            boolean finDeSemana = dia.getDayOfWeek().getValue() >= 5;
            int cantidad = (finDeSemana ? 10 : 6) + azar.nextInt(5);

            for (int k = 0; k < cantidad; k++) {
                LocalDateTime fecha = dia.atTime(horaDeServicio(azar), azar.nextInt(60));
                if (fecha.isAfter(ahora)) {
                    fecha = ahora.minusMinutes(5 + azar.nextInt(90));
                }
                // 1 de cada 5 pedidos va a nombre de un cliente registrado
                Cliente cliente = (k % 5 == 0) ? clientes.get(contadorCliente++ % clientes.size()) : null;
                Pedido pedido = crearPedido(fecha, EstadoPedido.PAGADO, MESEROS[azar.nextInt(MESEROS.length)],
                        null, cliente, itemsAleatorios(azar));
                Factura factura = facturaService.emitir(pedido);
                factura.setFecha(fecha);
            }
        }
    }

    /** Más peso al almuerzo (12-14) y a la cena (19-21). */
    private int horaDeServicio(Random azar) {
        int[] horas = {12, 13, 13, 14, 15, 18, 19, 19, 20, 20, 21};
        return horas[azar.nextInt(horas.length)];
    }

    private List<Item> itemsAleatorios(Random azar) {
        List<Item> items = new ArrayList<>();
        int lineas = 1 + azar.nextInt(4);
        for (int i = 0; i < lineas; i++) {
            Item base = CARTA.get(azar.nextInt(CARTA.size()));
            items.add(new Item(1 + azar.nextInt(3), base.descripcion(), base.precio()));
        }
        return items;
    }

    // ---------- mesas en servicio (mismo mapa del HTML) ----------

    private void cargarMesasEnServicio(Restaurante restaurante) {
        // número de mesa → minutos ocupada. Las de "esperando pago" son 3, 7 y 11.
        Map<Integer, Integer> minutos = Map.ofEntries(
                Map.entry(2, 45), Map.entry(3, 78), Map.entry(5, 12), Map.entry(6, 62), Map.entry(7, 52),
                Map.entry(10, 22), Map.entry(11, 33), Map.entry(12, 58), Map.entry(14, 34), Map.entry(16, 7));
        List<Integer> esperandoPago = List.of(3, 7, 11);

        for (Mesa mesa : restaurante.consultarMesas()) {
            Integer min = minutos.get(mesa.getNumero());
            if (min == null) {
                continue;
            }
            mesa.ocupar();
            mesa.setOcupadaDesde(Instant.now().minusSeconds(min * 60L));
            if (esperandoPago.contains(mesa.getNumero())) {
                mesa.solicitarPago();
            }
        }

        cuenta(restaurante, 3, "Carlos R.", EstadoPedido.SERVIDO, List.of(
                new Item(2, "Bandeja paisa (sin cebolla)", 32000), new Item(1, "Sancocho trifásico", 30000),
                new Item(2, "Limonada de coco", 9000), new Item(2, "Cerveza Club Colombia", 7000),
                new Item(1, "Tres leches", 11000)));
        cuenta(restaurante, 7, "Andrea P.", EstadoPedido.SERVIDO, List.of(
                new Item(1, "Ajiaco santafereño", 26000), new Item(1, "Limonada natural", 8000),
                new Item(1, "Cerveza Águila", 7000)));
        cuenta(restaurante, 11, "Carlos R.", EstadoPedido.SERVIDO, List.of(
                new Item(1, "Bandeja paisa", 32000), new Item(1, "Jugo de mora", 8000)));
        cuenta(restaurante, 2, "Luisa M.", EstadoPedido.EN_COCINA, List.of(
                new Item(2, "Arepa con queso", 6000), new Item(2, "Jugo de mora", 8000)));
        cuenta(restaurante, 5, "Andrea P.", EstadoPedido.ABIERTO, List.of(new Item(1, "Limonada natural", 8000)));
        cuenta(restaurante, 10, "Carlos R.", EstadoPedido.EN_COCINA, List.of(
                new Item(1, "Trucha al ajillo", 34000), new Item(1, "Cerveza Club Colombia", 7000)));
    }

    private void cuenta(Restaurante restaurante, int numeroMesa, String mesero, EstadoPedido estado, List<Item> items) {
        Mesa mesa = restaurante.consultarMesas().stream()
                .filter(m -> m.getNumero() == numeroMesa).findFirst().orElseThrow();
        crearPedido(LocalDateTime.now().minusMinutes(20), estado, mesero, mesa, null, items);
    }

    /** Deja variedad de estados DIAN como en el HTML: una rechazada y una pendiente hoy. */
    private void marcarFacturasDeEjemplo() {
        List<Factura> recientes = facturaRepository.findTop5ByOrderByFechaDesc();
        if (recientes.size() >= 3) {
            recientes.get(1).setEstado(EstadoFactura.PENDIENTE);
            Factura rechazada = recientes.get(2);
            rechazada.anularFactura();
            if (rechazada.getCliente() != null) {
                rechazada.getCliente().calcularTotalGastado();
            }
        }
    }

    // ---------- helper ----------

    private Pedido crearPedido(LocalDateTime fecha, EstadoPedido estado, String mesero, Mesa mesa,
                               Cliente cliente, List<Item> items) {
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido("PED-" + (++contadorPedidos));
        pedido.setFecha(fecha);
        pedido.setEstado(estado);
        pedido.setMesero(mesero);
        pedido.setMesa(mesa);
        pedido.setCliente(cliente);
        for (Item item : items) {
            DetallePedido d = new DetallePedido();
            d.setDescripcion(item.descripcion());
            d.setCantidad(item.cantidad());
            d.setPrecioUnitario(BigDecimal.valueOf(item.precio()));
            d.calcularSubtotal();
            pedido.agregarDetalle(d);
        }
        return pedidoRepository.save(pedido);
    }
}

# Foodi · Sistema de gestión de restaurante

Proyecto base Spring Boot 4.1.1 · Java 17+ · Maven · WAR · paquete `com.university.demo`.

## Cómo correrlo
1. Abrir la carpeta en IntelliJ como proyecto Maven (deja que descargue dependencias).
2. Ejecutar `DemoApplication`.
3. Abrir <http://localhost:8080> → pantalla de login del HTML.
   Usuarios demo (clave `foodi123`, se crean solos con el perfil `h2`):
   `jorge@lafondadelllano.com` (ADMINISTRADOR) · `andrea@lafondadelllano.com` (CAJERO) · `carlos@lafondadelllano.com` (MESERO)
4. Consola de la BD: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:foodidb`, usuario `sa`, sin clave).
5. Pruebas: `mvn test` (unitarias del dominio + `FlujoServiciosTest`, que prueba los casos de uso con JPA/H2 reales).

## Estructura
```
src/main/java/com/university/demo
├── domain          Entidades del diagrama de clases + enums + Auditable
├── application
│   ├── dto         Records de entrada/salida (con validaciones)
│   ├── mapper      Entidad <-> DTO
│   └── service     Casos de uso (un servicio por módulo)
├── infrastructure
│   ├── config      JpaAuditing, PasswordConfig (BCrypt), WebConfig + SesionInterceptor, DataLoader (perfil h2)
│   └── repository  Un JpaRepository por entidad (con consultas derivadas)
└── presentation
    ├── controller  Un controlador REST por módulo
    └── exception   GlobalExceptionHandler (400 / 404 / 409) + RecursoNoEncontradoException
src/main/resources/static   index.html + css/foodi.css + js/foodi.js (front conectado a la API)
src/test/java               Unitarias: PedidoTest, IngredienteTest, FacturaTest, MesaTest · Integración: FlujoServiciosTest
```

## Seguridad
Toda la API `/api/**` exige sesión iniciada (401 si no). `/api/usuarios` solo para el rol ADMINISTRADOR (403).
Contraseñas con BCrypt. Es una protección por sesión sin Spring Security; migrar a Spring Security queda como mejora.

## Endpoints
| Módulo | Método y ruta | Qué hace |
|---|---|---|
| Auth | POST `/api/auth/login` · POST `/api/auth/logout` · GET `/api/auth/me` | Sesión |
| Usuarios | GET/POST `/api/usuarios` | Listar / crear (solo admin) |
| Dashboard | GET `/api/dashboard` | KPIs del día, ventas de 7 días vs. IA, última predicción |
| Predicción IA | GET `/api/prediccion` · POST `/api/prediccion/generar` | Última / recalcular la de mañana |
| Mesas | GET `/api/mesas` · GET `/{id}` · POST (crear) | Mapa de mesas con su pedido activo |
| | POST `/api/mesas/{id}/ocupar` · `/solicitar-pago` · `/liberar` · `/cobrar` | Ciclo de la mesa. `cobrar` cierra el pedido, emite y valida la factura |
| Pedidos | GET `/api/pedidos` · GET `/{id}` · POST (abrir cuenta) | |
| | POST `/{id}/detalles` · PUT/DELETE `/{id}/detalles/{detalleId}` · PATCH `/{id}/estado` | Ítems y estado (ABIERTO, EN_COCINA, SERVIDO) |
| Facturas | GET `/api/facturas?q=` · GET `/ultimas` · GET `/resumen` · POST (desde pedido) | |
| | POST `/{id}/validar-dian` · POST `/{id}/anular` | |
| Clientes | GET `/api/clientes?q=` · GET/PUT `/{id}` · POST · GET `/{id}/historial` · POST `/{id}/recalcular-total` | |
| Inventario | GET `/api/inventario/ingredientes` · GET `/alertas` · POST `/ingredientes` | Stock y alertas de stock mínimo |
| | GET/POST `/api/inventario/ingredientes/{id}/movimientos` | ENTRADA / MERMA / AJUSTE |
| Proveedores | GET/POST `/api/proveedores` · PUT `/{id}` · GET `/{id}/productos` | |

## Dónde quedó cada método del diagrama de clases
| Clase | Métodos | Implementación |
|---|---|---|
| Usuario / Autenticacion | iniciarSesion, cerrarSesion, validarCredenciales, verificarSesion | `AutenticacionService` + `AuthController` |
| Restaurante | registrarMesa, consultarMesas, consultarVentas | `Restaurante` · `MesaService.crear` |
| Mesa | ocupar, liberar, solicitarPago, consultarEstado, tiempoOcupacion | `Mesa` · `MesaService` |
| Pedido / DetallePedido | agregarDetalle, eliminarDetalle, calcularSubtotal/IVA/Propina/Total, cambiarEstado, modificarCantidad | `Pedido`, `DetallePedido` · `PedidoService` |
| Factura | generarFactura, calcularTotal, validarDIAN, anularFactura | `Factura` · `FacturaService` |
| Cliente | registrarCliente, actualizarDatos, consultarHistorial, calcularTotalGastado, incrementarVisitas | `Cliente` · `ClienteService` |
| Inventario / Ingrediente | agregarIngrediente, consultarStock, verificarStockMinimo, actualizarStock, verificarStock, calcularCosto | `Inventario`, `Ingrediente` · `InventarioService` |
| MovimientoInventario | registrar, aplicarMovimiento | `MovimientoInventario` · `InventarioService.registrarMovimiento` |
| Proveedor | registrarProveedor, actualizarDatos, consultarProductos | `Proveedor` · `ProveedorService` |
| PrediccionIA | generarPrediccion, calcularConfianza, identificarHoraPico, generarRecomendaciones | `PrediccionIAService` |
| Dashboard | mostrarIndicadores | `DashboardService` |

## Motor de predicción (PrediccionIAService)
Estadístico, sin librerías externas: promedia las ventas de los mismos días de la semana (últimas 8 semanas), ajusta por tendencia
(últimos 7 días vs. los 7 anteriores, tope ±20 %), calcula la confianza según cantidad y variabilidad de los datos, detecta la hora pico
y arma recomendaciones (personal extra, compras urgentes por stock crítico).

## Reglas de modelo y JPA (leer antes de tocar las entidades)
- Todos los estados son enums guardados como texto (`EnumType.STRING`): `EstadoPedido`, `EstadoMesa`, `EstadoFactura`, `EstadoStock`, `TipoMovimiento`.
- Restricciones únicas: `mesas(restaurante_id, numero)`, `predicciones_ia(fecha)`, `clientes(documento)` (un documento vacío se guarda como `null`).
- **Entidad nueva → `repository.save(entidad)` de ESA entidad.** No hagas `saveAndFlush(padre)` para guardar un hijo nuevo: el `merge` en cascada guarda una copia y tu instancia queda sin `id`.
- **Entidad ya cargada (gestionada) → no necesita `save`**; basta `flush()` si necesitas el `id` de un hijo recién agregado.
- No hagas `cliente.getFacturas().add(..)` a ciegas: en una colección inversa perezosa se encola y puede duplicar el elemento al inicializarse.
- Solo los pedidos `PAGADO` cuentan como ventas (dashboard y predicción). Facturar un pedido (`emitir`) lo cierra y libera su mesa.
- La propina va en el pedido pero no en la factura (factura = subtotal + IVA).

## Pendiente / mejoras posibles
- Definir la base de datos real: crear `application-<motor>.properties`, agregar el driver en el `pom.xml` y cambiar `spring.profiles.active`.
- Spring Security completo (hoy: sesión + interceptor).
- Integración real con la DIAN (`Factura.validarDIAN()` sigue siendo una simulación).
- El selector Semana/Mes/Trimestre del gráfico solo muestra la semana.

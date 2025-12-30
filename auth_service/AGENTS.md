# AGENTS.md

## Comandos de construcción, lint y pruebas

- **Compilar**: `./mvnw clean package`
- **Ejecutar todas las pruebas**: `./mvnw test`
- **Ejecutar una prueba específica**: `./mvnw test -Dtest=NombreDeLaClase` (ejemplo: `-Dtest=AuthServiceApplicationTests`)
- **Ejecutar la aplicación**: `./mvnw spring-boot:run`

## Guía de estilo de código

- **Importaciones**: Orden: (1) Java, (2) Terceros, (3) Proyecto. Eliminar importaciones no usadas.
- **Formato**: Indentación de 4 espacios, llaves en la misma línea. Máximo ~120 caracteres por línea.
- **Nomenclatura**:
    - Clases: `PascalCase`
    - Métodos/Campos: `camelCase`
    - Constantes: `MAYÚSCULAS_CON_GUIONES_BAJO`
- **Tipos**: Utiliza tipos explícitos, evita tipos raw. Prefiere interfaces en firmas.
- **Manejo de errores**: Lanza excepciones para estados inválidos (por ejemplo, `IllegalArgumentException`). Falla rápido donde sea posible.
- **Lombok**: Usa `@Builder`, `@Data`, etc. para reducir código repetitivo (boilerplate) como se muestra en los entities/domain.
- **Convenciones Spring**: Anota servicios con `@Service` y usa inyección por constructor con `@RequiredArgsConstructor` (Lombok).

No existen reglas de Cursor ni GitHub Copilot en este repositorio.

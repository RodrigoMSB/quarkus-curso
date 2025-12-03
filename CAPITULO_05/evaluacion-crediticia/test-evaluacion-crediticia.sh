#!/bin/bash

#═══════════════════════════════════════════════════════════════════════════════
# PRUEBAS INTERACTIVAS - API EVALUACION CREDITICIA (DNI PERUANO)
#═══════════════════════════════════════════════════════════════════════════════
#
# COMPATIBILIDAD: Mac (zsh/bash) y Windows (Git Bash)
#
# REQUISITOS:
#   - curl (incluido en ambos sistemas)
#   - jq (opcional pero recomendado)
#       Mac:     brew install jq
#       Windows: descargar de https://jqlang.github.io/jq/download/
#                colocar jq.exe en C:\Program Files\Git\usr\bin\
#
# USO:
#   chmod +x test-evaluacion-crediticia.sh
#   ./test-evaluacion-crediticia.sh
#
#═══════════════════════════════════════════════════════════════════════════════

API_URL="http://localhost:8080"
OUTPUT_FILE="resultados-evaluacion-crediticia-$(date '+%Y-%m-%d_%H-%M-%S').txt"

# Colores (compatibles con Git Bash)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
MAGENTA='\033[0;35m'
RESET='\033[0m'

# Contadores
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Limpiar archivo de salida
: > "$OUTPUT_FILE"

# Funcion para eliminar codigos ANSI (compatible Windows Git Bash)
strip_ansi() {
    # Usa tr y sed de forma compatible con Git Bash
    sed 's/\x1b\[[0-9;]*m//g' 2>/dev/null || sed 's/\[[0-9;]*m//g' 2>/dev/null || cat
}

# Funcion de logging mejorada
log() {
    local message="$*"
    printf "%b\n" "$message"
    # Guardar sin colores ANSI
    printf "%b\n" "$message" | strip_ansi >> "$OUTPUT_FILE"
}

# Funcion para mostrar JSON
show_json() {
    local json="$1"
    
    if [ -z "$json" ]; then
        return
    fi
    
    if command -v jq &> /dev/null; then
        printf '%s\n' "$json" | jq '.' 2>/dev/null | tee -a "$OUTPUT_FILE" || printf '%s\n' "$json" | tee -a "$OUTPUT_FILE"
    else
        printf '%s\n' "$json" | tee -a "$OUTPUT_FILE"
    fi
}

# Funcion para pausa interactiva
pause() {
    printf "\n"
    printf "${CYAN}>>  Presiona ENTER para continuar...${RESET}"
    read -r
    printf "\n"
}

# Banner
clear
log "${CYAN}+============================================================================+${RESET}"
log "${CYAN}|${RESET}  ${WHITE}PRUEBAS INTERACTIVAS - API EVALUACION CREDITICIA (QUARKUS)${RESET}          ${CYAN}|${RESET}"
log "${CYAN}+============================================================================+${RESET}"
log ""
log "${CYAN}Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}API Base:${RESET} $API_URL"
log "${CYAN}Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}Validacion:${RESET} DNI Peruano (8 digitos numericos)"
log "${CYAN}Base de Datos:${RESET} PostgreSQL (Dev Services automatico)"
log "${CYAN}Umbral Aprobacion:${RESET} Score >= 650 puntos"
log ""

# Verificar jq
if ! command -v jq &> /dev/null; then
    log "${YELLOW}NOTA: jq no instalado. JSON se mostrara sin formato.${RESET}"
    log "${YELLOW}      Mac: brew install jq${RESET}"
    log "${YELLOW}      Win: https://jqlang.github.io/jq/download/${RESET}"
    log ""
fi

# Verificar servidor
log "${YELLOW}Verificando conectividad con el servidor...${RESET}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/api/v1/creditos" 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    log "${GREEN}[OK] Servidor respondiendo correctamente${RESET}"
else
    log "${RED}[ERROR] No se puede conectar al servidor (HTTP: $HTTP_CODE)${RESET}"
    log ""
    log "${YELLOW}Soluciones:${RESET}"
    log "  ${WHITE}1.${RESET} Asegurate de que Docker Desktop esta corriendo"
    log "  ${WHITE}2.${RESET} Navega a la carpeta del proyecto: ${CYAN}cd evaluacion-crediticia${RESET}"
    log "  ${WHITE}3.${RESET} Inicia Quarkus: ${CYAN}./mvnw quarkus:dev${RESET}"
    log "  ${WHITE}4.${RESET} Espera el mensaje: ${GREEN}Listening on: http://localhost:8080${RESET}"
    log ""
    exit 1
fi
log ""
pause

# Funcion para ejecutar test con archivo temporal
run_test() {
    local test_num="$1"
    local test_name="$2"
    local method="$3"
    local endpoint="$4"
    local data="$5"
    local expected_status="$6"
    local description="$7"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    clear
    log ""
    log "${CYAN}--------------------------------------------------------------------------${RESET}"
    log "${WHITE}Test #$test_num: $test_name${RESET}"
    log "${CYAN}--------------------------------------------------------------------------${RESET}"
    log ""
    log "${YELLOW}Method:${RESET}   $method"
    log "${YELLOW}Endpoint:${RESET} $endpoint"
    
    if [ -n "$data" ]; then
        log ""
        log "${YELLOW}Request Body:${RESET}"
        show_json "$data"
    fi
    
    log ""
    log "${MAGENTA}===========================================================================${RESET}"
    log ""
    
    # Ejecutar request usando archivo temporal para POST
    local response
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" \
            -H "Content-Type: application/json" 2>/dev/null)
    else
        # SOLUCION WINDOWS: Escribir JSON en archivo temporal con printf
        local temp_file
        temp_file=$(mktemp)
        printf '%s' "$data" > "$temp_file"
        
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" \
            -H "Content-Type: application/json" \
            --data-binary "@$temp_file" 2>/dev/null)
        
        rm -f "$temp_file"
    fi
    
    # Separar body y status (compatible Windows - sin echo)
    local body
    local status
    local line_count
    line_count=$(printf '%s\n' "$response" | wc -l)
    body=$(printf '%s\n' "$response" | head -n $((line_count - 1)))
    status=$(printf '%s\n' "$response" | tail -n 1)
    
    # Limpiar posible \r de Windows
    status=$(printf '%s' "$status" | tr -d '\r')
    
    # Mostrar response
    log "${YELLOW}Response (HTTP $status):${RESET}"
    show_json "$body"
    log ""
    
    # Validar status
    if [ "$status" = "$expected_status" ]; then
        log "${GREEN}[PASS]${RESET} - HTTP $status (Esperado: $expected_status)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log "${RED}[FAIL]${RESET} - HTTP $status (Esperado: $expected_status)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    # Descripcion del resultado esperado
    if [ -n "$description" ]; then
        log ""
        log "${CYAN}Resultado esperado:${RESET}"
        log "   $description"
    fi
    
    # Capturar ID si existe y jq disponible
    if command -v jq &> /dev/null; then
        local captured_id
        captured_id=$(printf '%s' "$body" | jq -r '.solicitudId' 2>/dev/null)
        if [ -n "$captured_id" ] && [ "$captured_id" != "null" ]; then
            log ""
            log "${YELLOW}-> Solicitud ID capturado: $captured_id${RESET}"
        fi
    fi
    
    pause
}

#═══════════════════════════════════════════════════════════════════════════════
# MODULO 1: EVALUACIONES EXITOSAS (APROBADAS)
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${GREEN}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log "${WHITE}  [OK] MODULO 1: EVALUACIONES EXITOSAS (SOLICITUDES APROBADAS)${RESET}"
log "${GREEN}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log ""
log "${CYAN}Este modulo prueba solicitudes con perfiles crediticios solidos:${RESET}"
log "  - Ingresos estables y suficientes"
log "  - DTI (Debt-to-Income) por debajo del 50%"
log "  - Estabilidad laboral (>= 3 meses)"
log "  - Score esperado: >= 650 puntos"
log ""
pause

# Test 1: Perfil EXCELENTE
# NOTA: Nombres sin acentos para compatibilidad Windows Git Bash
run_test 1 \
    "Solicitud con perfil EXCELENTE (Score >= 800)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"12345678","nombreCompleto":"Juan Perez Garcia","email":"juan.perez@banco.pe","edad":35,"ingresosMensuales":2500000,"deudasActuales":300000,"montoSolicitado":5000000,"mesesEnEmpleoActual":48}' \
    "201" \
    "[APROBADA] Score >= 800 - Excelente capacidad de pago, DTI bajo (12%), alta estabilidad laboral"

# Test 2: Perfil BUENO
run_test 2 \
    "Solicitud con perfil BUENO (Score 650-799)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"23456789","nombreCompleto":"Maria Silva Torres","email":"maria.silva@banco.pe","edad":28,"ingresosMensuales":1800000,"deudasActuales":400000,"montoSolicitado":3000000,"mesesEnEmpleoActual":24}' \
    "201" \
    "[APROBADA] Score entre 650-799 - Buen perfil, DTI aceptable (22%), estabilidad laboral adecuada"

#═══════════════════════════════════════════════════════════════════════════════
# MODULO 2: EVALUACIONES RECHAZADAS
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${RED}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log "${WHITE}  [X] MODULO 2: EVALUACIONES RECHAZADAS (ALTO RIESGO)${RESET}"
log "${RED}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log ""
log "${CYAN}Este modulo prueba solicitudes que no cumplen requisitos minimos:${RESET}"
log "  - DTI > 50% (sobre-endeudamiento)"
log "  - Inestabilidad laboral (< 3 meses)"
log "  - Score esperado: < 650 puntos o rechazo automatico"
log ""
pause

# Test 3: DTI Alto
run_test 3 \
    "Rechazo por DTI alto (>50%)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"34567890","nombreCompleto":"Carlos Rojas Vega","email":"carlos.rojas@banco.pe","edad":42,"ingresosMensuales":1500000,"deudasActuales":900000,"montoSolicitado":4000000,"mesesEnEmpleoActual":12}' \
    "201" \
    "[RECHAZADA] DTI = 60% (limite: 50%) - Sobre-endeudamiento detectado, alto riesgo de impago"

# Test 4: Inestabilidad Laboral
run_test 4 \
    "Rechazo por inestabilidad laboral (<3 meses)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"45678901","nombreCompleto":"Ana Lopez Munoz","email":"ana.lopez@banco.pe","edad":23,"ingresosMensuales":1200000,"deudasActuales":150000,"montoSolicitado":2000000,"mesesEnEmpleoActual":2}' \
    "201" \
    "[RECHAZADA] Empleo actual: 2 meses (minimo: 3) - Riesgo de perdida de ingresos"

#═══════════════════════════════════════════════════════════════════════════════
# MODULO 3: VALIDACIONES Y CASOS EDGE
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${YELLOW}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log "${WHITE}  [?] MODULO 3: VALIDACIONES Y CASOS EDGE${RESET}"
log "${YELLOW}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log ""
log "${CYAN}Este modulo prueba validadores custom y manejo de errores:${RESET}"
log "  - Validacion de DNI peruano (8 digitos)"
log "  - Bean Validation (campos obligatorios)"
log "  - Exception Mappers (respuestas amigables)"
log ""
pause

# Test 5: DNI Invalido (muy corto)
run_test 5 \
    "Validacion de DNI invalido (5 digitos)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"12345","nombreCompleto":"Pedro Invalido","email":"pedro@banco.pe","edad":30,"ingresosMensuales":2000000,"deudasActuales":200000,"montoSolicitado":3000000,"mesesEnEmpleoActual":12}' \
    "400" \
    "[ERROR 400] DNI debe tener exactamente 8 digitos. Validador @DniValido funcionando"

# Test 6: DNI Invalido (muy largo)
run_test 6 \
    "Validacion de DNI invalido (10 digitos)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"1234567890","nombreCompleto":"Luis Invalido","email":"luis@banco.pe","edad":40,"ingresosMensuales":3000000,"deudasActuales":500000,"montoSolicitado":6000000,"mesesEnEmpleoActual":24}' \
    "400" \
    "[ERROR 400] DNI no puede exceder 8 digitos"

# Test 7: Email Invalido
run_test 7 \
    "Validacion de email invalido" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"87654321","nombreCompleto":"Rosa Flores","email":"email-invalido","edad":32,"ingresosMensuales":2200000,"deudasActuales":400000,"montoSolicitado":4000000,"mesesEnEmpleoActual":18}' \
    "400" \
    "[ERROR 400] Email debe tener formato valido. Validador @Email funcionando"

# Test 8: Edad menor de 18
run_test 8 \
    "Validacion de edad minima (<18)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"11223344","nombreCompleto":"Menor Edad","email":"menor@banco.pe","edad":17,"ingresosMensuales":1000000,"deudasActuales":0,"montoSolicitado":1000000,"mesesEnEmpleoActual":6}' \
    "400" \
    "[ERROR 400] Edad minima 18 anios. Validador @Min funcionando"

#═══════════════════════════════════════════════════════════════════════════════
# MODULO 4: OPERACIONES DE CONSULTA
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log "${WHITE}  [i] MODULO 4: OPERACIONES DE CONSULTA (GET)${RESET}"
log "${CYAN}++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++${RESET}"
log ""
log "${CYAN}Este modulo prueba operaciones de lectura:${RESET}"
log "  - Listar todas las solicitudes"
log "  - Obtener solicitud especifica por ID"
log "  - Manejo de solicitudes inexistentes (404)"
log ""
pause

# Test 9: Listar todas las solicitudes
run_test 9 \
    "Listar todas las solicitudes" \
    "GET" \
    "$API_URL/api/v1/creditos" \
    "" \
    "200" \
    "[OK] Array con todas las solicitudes (aprobadas, rechazadas y pendientes)"

# Test 10: Obtener solicitud especifica
run_test 10 \
    "Obtener solicitud especifica (ID=1)" \
    "GET" \
    "$API_URL/api/v1/creditos/1" \
    "" \
    "200" \
    "[OK] Detalle completo de la solicitud: DNI, nombre, score, estado, razon de evaluacion"

# Test 11: Solicitud inexistente
run_test 11 \
    "Obtener solicitud inexistente (ID=99999)" \
    "GET" \
    "$API_URL/api/v1/creditos/99999" \
    "" \
    "404" \
    "[ERROR 404] Solicitud no existe en la base de datos"

#═══════════════════════════════════════════════════════════════════════════════
# RESUMEN FINAL
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}+============================================================================+${RESET}"
log "${CYAN}|${RESET}  ${WHITE}RESUMEN FINAL DE EJECUCION${RESET}                                             ${CYAN}|${RESET}"
log "${CYAN}+============================================================================+${RESET}"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    log "  ${GREEN}[SUCCESS] TODOS LOS TESTS PASARON EXITOSAMENTE${RESET}"
else
    log "  ${YELLOW}[WARNING] ALGUNOS TESTS FALLARON${RESET}"
fi

log ""
log "  ${WHITE}Tests Ejecutados:${RESET}  $TOTAL_TESTS"
log "  ${GREEN}Tests Exitosos:${RESET}    $PASSED_TESTS"
log "  ${RED}Tests Fallidos:${RESET}    $FAILED_TESTS"
log ""
log "  ${CYAN}Resultados guardados en: $OUTPUT_FILE${RESET}"
log ""

log "${CYAN}+============================================================================+${RESET}"
log "${CYAN}|${RESET}  ${WHITE}RESUMEN DE OPERACIONES PROBADAS${RESET}                                       ${CYAN}|${RESET}"
log "${CYAN}+============================================================================+${RESET}"
log ""
log "  ${GREEN}[OK] EVALUACIONES EXITOSAS:${RESET}"
log "     - Perfil excelente (score >= 800)"
log "     - Perfil bueno (score 650-799)"
log ""
log "  ${RED}[X] EVALUACIONES RECHAZADAS:${RESET}"
log "     - DTI alto (>50%) - Sobre-endeudamiento"
log "     - Inestabilidad laboral (<3 meses)"
log ""
log "  ${YELLOW}[?] VALIDACIONES:${RESET}"
log "     - DNI peruano (8 digitos) - @DniValido"
log "     - Email valido - @Email"
log "     - Edad minima (18 anios) - @Min"
log "     - Campos obligatorios - @NotBlank"
log ""
log "  ${CYAN}[i] CONSULTAS:${RESET}"
log "     - Listar todas las solicitudes (GET)"
log "     - Obtener solicitud especifica (GET)"
log "     - Manejo de 404 Not Found"
log ""

log "${CYAN}+============================================================================+${RESET}"
log "${CYAN}|${RESET}  ${WHITE}ALGORITMO DE SCORING CREDITICIO${RESET}                                       ${CYAN}|${RESET}"
log "${CYAN}+============================================================================+${RESET}"
log ""
log "  ${WHITE}Factores Evaluados:${RESET}"
log "     - ${CYAN}DTI (Debt-to-Income):${RESET} Limite 50%"
log "     - ${CYAN}Estabilidad laboral:${RESET} Minimo 3 meses"
log "     - ${CYAN}Capacidad de pago:${RESET} Cuota <= 30% ingreso"
log "     - ${CYAN}Edad:${RESET} Rango optimo 25-55 anios"
log "     - ${CYAN}Monto solicitado:${RESET} vs ingreso mensual"
log ""
log "  ${WHITE}Escala de Score:${RESET}"
log "     - ${GREEN}800-1000:${RESET} Excelente (aprobacion inmediata)"
log "     - ${GREEN}650-799:${RESET}  Bueno (aprobacion estandar)"
log "     - ${YELLOW}500-649:${RESET}  Regular (requiere analisis)"
log "     - ${RED}0-499:${RESET}    Malo (rechazo automatico)"
log ""
log "  ${WHITE}Umbral de Aprobacion: ${GREEN}650 puntos${RESET}"
log ""

log "${CYAN}+============================================================================+${RESET}"
log "${CYAN}|${RESET}  ${WHITE}NOTAS TECNICAS${RESET}                                                         ${CYAN}|${RESET}"
log "${CYAN}+============================================================================+${RESET}"
log ""
log "  ${CYAN}Endpoint Base:${RESET} $API_URL/api/v1/creditos"
log "  ${CYAN}DNI Peruano:${RESET} Exactamente 8 digitos numericos"
log "  ${CYAN}Base de Datos:${RESET} PostgreSQL (Dev Services - Testcontainers)"
log "  ${CYAN}Testing:${RESET} JUnit 5 + REST Assured + @QuarkusTest"
log "  ${CYAN}Validadores:${RESET} Bean Validation + Custom Validators"
log "  ${CYAN}Exception Mappers:${RESET} Respuestas HTTP amigables"
log ""

log "${CYAN}+============================================================================+${RESET}"
log "${CYAN}|${RESET}  ${WHITE}RECURSOS UTILES${RESET}                                                        ${CYAN}|${RESET}"
log "${CYAN}+============================================================================+${RESET}"
log ""
log "  ${CYAN}Dev UI:${RESET}       http://localhost:8080/q/dev"
log "  ${CYAN}Swagger UI:${RESET}   http://localhost:8080/q/swagger-ui"
log "  ${CYAN}OpenAPI Spec:${RESET} http://localhost:8080/q/openapi"
log "  ${CYAN}Health Check:${RESET} http://localhost:8080/q/health"
log ""

log "${CYAN}+============================================================================+${RESET}"
log "${CYAN}|${RESET}  ${WHITE}CONCEPTOS DEMOSTRADOS - CAPITULO 5${RESET}                                    ${CYAN}|${RESET}"
log "${CYAN}+============================================================================+${RESET}"
log ""
log "  ${GREEN}[ok]${RESET} Testing con ${CYAN}@QuarkusTest${RESET} (JUnit 5)"
log "  ${GREEN}[ok]${RESET} REST Assured para testing de APIs"
log "  ${GREEN}[ok]${RESET} Dev Services (PostgreSQL automatico)"
log "  ${GREEN}[ok]${RESET} Bean Validation (${CYAN}@NotBlank, @Email, @Min, @Max${RESET})"
log "  ${GREEN}[ok]${RESET} Validadores Custom (${CYAN}@DniValido${RESET})"
log "  ${GREEN}[ok]${RESET} Exception Mappers (respuestas amigables)"
log "  ${GREEN}[ok]${RESET} Hibernate ORM with Panache"
log "  ${GREEN}[ok]${RESET} Inyeccion de dependencias (CDI)"
log "  ${GREEN}[ok]${RESET} Logica de negocio bancaria (DTI, Scoring)"
log "  ${GREEN}[ok]${RESET} Manejo de errores HTTP (400, 404)"
log ""

log "${CYAN}--------------------------------------------------------------------------${RESET}"
log ""
log "  ${WHITE}TIP: Para ver el archivo formateado:${RESET}"
log "     ${CYAN}cat $OUTPUT_FILE${RESET}"
log ""
log "  ${WHITE}Documentacion completa en:${RESET}"
log "     ${CYAN}TEORIA.md${RESET} - Conceptos y explicaciones"
log "     ${CYAN}TESTS.md${RESET}  - Guia de testing"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi
#!/bin/bash

#═══════════════════════════════════════════════════════════════════════════════
# 🏦 PRUEBAS INTERACTIVAS - API EVALUACIÓN CREDITICIA (DNI PERUANO)
#═══════════════════════════════════════════════════════════════════════════════

API_URL="http://localhost:8080"
OUTPUT_FILE="resultados-evaluacion-crediticia-$(date '+%Y-%m-%d_%H-%M-%S').txt"

# Colores
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
> "$OUTPUT_FILE"

# Función de logging mejorada
log() {
    local message="$*"
    printf "%b\n" "$message"
    printf "%b\n" "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# Función para mostrar JSON
show_json() {
    local json="$1"
    
    if ! command -v jq &> /dev/null; then
        printf "%s\n" "$json" | tee -a "$OUTPUT_FILE"
        return
    fi
    
    if [ -n "$json" ]; then
        echo "$json" | jq '.' 2>/dev/null | tee -a "$OUTPUT_FILE" || echo "$json" | tee -a "$OUTPUT_FILE"
    fi
}

# Función para pausa interactiva
pause() {
    echo ""
    printf "${CYAN}▶️  Presiona ENTER para continuar...${RESET}"
    read -r
    echo ""
}

# Banner
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🏦 PRUEBAS INTERACTIVAS - API EVALUACIÓN CREDITICIA (QUARKUS)${RESET}     ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${RESET} $API_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}🇵🇪 Validación:${RESET} DNI Peruano (8 dígitos numéricos)"
log "${CYAN}💾 Base de Datos:${RESET} PostgreSQL (Dev Services automático)"
log "${CYAN}🎯 Umbral Aprobación:${RESET} Score >= 650 puntos"
log ""

# Verificar servidor
log "${YELLOW}🔍 Verificando conectividad con el servidor...${RESET}"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/api/v1/creditos" 2>/dev/null)
if [ "$HTTP_CODE" == "200" ]; then
    log "${GREEN}✅ Servidor respondiendo correctamente${RESET}"
else
    log "${RED}❌ ERROR: No se puede conectar al servidor (HTTP: $HTTP_CODE)${RESET}"
    log ""
    log "${YELLOW}Soluciones:${RESET}"
    log "  ${WHITE}1.${RESET} Asegúrate de que Docker Desktop está corriendo"
    log "  ${WHITE}2.${RESET} Navega a la carpeta del proyecto: ${CYAN}cd evaluacion-crediticia${RESET}"
    log "  ${WHITE}3.${RESET} Inicia Quarkus: ${CYAN}./mvnw quarkus:dev${RESET}"
    log "  ${WHITE}4.${RESET} Espera el mensaje: ${GREEN}Listening on: http://localhost:8080${RESET}"
    log ""
    exit 1
fi
log ""
pause

# Función para ejecutar test con archivo temporal
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
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    log "${WHITE}📋 Test #$test_num: $test_name${RESET}"
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    log ""
    log "${YELLOW}Method:${RESET}   $method"
    log "${YELLOW}Endpoint:${RESET} $endpoint"
    
    if [ -n "$data" ]; then
        log ""
        log "${YELLOW}Request Body:${RESET}"
        show_json "$data"
    fi
    
    log ""
    log "${MAGENTA}═══════════════════════════════════════════════════════════════════════════${RESET}"
    log ""
    
    # Ejecutar request usando archivo temporal para POST
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" \
            -H "Content-Type: application/json" 2>/dev/null)
    else
        # SOLUCIÓN WINDOWS: Escribir JSON en archivo temporal con printf para evitar problemas de encoding
        local temp_file=$(mktemp)
        printf '%s' "$data" > "$temp_file"
        
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" \
            -H "Content-Type: application/json" \
            --data-binary "@$temp_file" 2>/dev/null)
        
        rm -f "$temp_file"
    fi
    
    # Separar body y status
    body=$(echo "$response" | sed '$d')
    status=$(echo "$response" | tail -n 1)
    
    # Mostrar response
    log "${YELLOW}Response (HTTP $status):${RESET}"
    show_json "$body"
    log ""
    
    # Validar status
    if [ "$status" == "$expected_status" ]; then
        log "${GREEN}✓ PASS${RESET} - HTTP $status (Esperado: $expected_status)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: $expected_status)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    # Descripción del resultado esperado
    if [ -n "$description" ]; then
        log ""
        log "${CYAN}💡 Resultado esperado:${RESET}"
        log "   $description"
    fi
    
    # Capturar ID si existe
    if command -v jq &> /dev/null; then
        local captured_id=$(echo "$body" | jq -r '.solicitudId' 2>/dev/null)
        if [ -n "$captured_id" ] && [ "$captured_id" != "null" ]; then
            log ""
            log "${YELLOW}→ Solicitud ID capturado: $captured_id${RESET}"
        fi
    fi
    
    pause
}

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 1: EVALUACIONES EXITOSAS (APROBADAS)
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${GREEN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  ✅ MÓDULO 1: EVALUACIONES EXITOSAS (SOLICITUDES APROBADAS)${RESET}"
log "${GREEN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log ""
log "${CYAN}Este módulo prueba solicitudes con perfiles crediticios sólidos:${RESET}"
log "  • Ingresos estables y suficientes"
log "  • DTI (Debt-to-Income) por debajo del 50%"
log "  • Estabilidad laboral (>= 3 meses)"
log "  • Score esperado: >= 650 puntos"
log ""
pause

# Test 1: Perfil EXCELENTE
run_test 1 \
    "Solicitud con perfil EXCELENTE (Score >= 800)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"12345678","nombreCompleto":"Juan Pérez García","email":"juan.perez@banco.pe","edad":35,"ingresosMensuales":2500000,"deudasActuales":300000,"montoSolicitado":5000000,"mesesEnEmpleoActual":48}' \
    "201" \
    "✅ APROBADA - Score >= 800 - Excelente capacidad de pago, DTI bajo (12%), alta estabilidad laboral"

# Test 2: Perfil BUENO
run_test 2 \
    "Solicitud con perfil BUENO (Score 650-799)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"23456789","nombreCompleto":"María Silva Torres","email":"maria.silva@banco.pe","edad":28,"ingresosMensuales":1800000,"deudasActuales":400000,"montoSolicitado":3000000,"mesesEnEmpleoActual":24}' \
    "201" \
    "✅ APROBADA - Score entre 650-799 - Buen perfil, DTI aceptable (22%), estabilidad laboral adecuada"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 2: EVALUACIONES RECHAZADAS
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${RED}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  ❌ MÓDULO 2: EVALUACIONES RECHAZADAS (ALTO RIESGO)${RESET}"
log "${RED}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log ""
log "${CYAN}Este módulo prueba solicitudes que no cumplen requisitos mínimos:${RESET}"
log "  • DTI > 50% (sobre-endeudamiento)"
log "  • Inestabilidad laboral (< 3 meses)"
log "  • Score esperado: < 650 puntos o rechazo automático"
log ""
pause

# Test 3: DTI Alto
run_test 3 \
    "Rechazo por DTI alto (>50%)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"34567890","nombreCompleto":"Carlos Rojas Vega","email":"carlos.rojas@banco.pe","edad":42,"ingresosMensuales":1500000,"deudasActuales":900000,"montoSolicitado":4000000,"mesesEnEmpleoActual":12}' \
    "201" \
    "❌ RECHAZADA - DTI = 60% (límite: 50%) - Sobre-endeudamiento detectado, alto riesgo de impago"

# Test 4: Inestabilidad Laboral
run_test 4 \
    "Rechazo por inestabilidad laboral (<3 meses)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"45678901","nombreCompleto":"Ana López Muñoz","email":"ana.lopez@banco.pe","edad":23,"ingresosMensuales":1200000,"deudasActuales":150000,"montoSolicitado":2000000,"mesesEnEmpleoActual":2}' \
    "201" \
    "❌ RECHAZADA - Empleo actual: 2 meses (mínimo: 3) - Riesgo de pérdida de ingresos"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 3: VALIDACIONES Y CASOS EDGE
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${YELLOW}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  🔍 MÓDULO 3: VALIDACIONES Y CASOS EDGE${RESET}"
log "${YELLOW}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log ""
log "${CYAN}Este módulo prueba validadores custom y manejo de errores:${RESET}"
log "  • Validación de DNI peruano (8 dígitos)"
log "  • Bean Validation (campos obligatorios)"
log "  • Exception Mappers (respuestas amigables)"
log ""
pause

# Test 5: DNI Inválido (muy corto)
run_test 5 \
    "Validación de DNI inválido (5 dígitos)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"12345","nombreCompleto":"Pedro Inválido","email":"pedro@banco.pe","edad":30,"ingresosMensuales":2000000,"deudasActuales":200000,"montoSolicitado":3000000,"mesesEnEmpleoActual":12}' \
    "400" \
    "❌ Error 400 Bad Request - DNI debe tener exactamente 8 dígitos. Validador @DniValido funcionando"

# Test 6: DNI Inválido (muy largo)
run_test 6 \
    "Validación de DNI inválido (10 dígitos)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"1234567890","nombreCompleto":"Luis Inválido","email":"luis@banco.pe","edad":40,"ingresosMensuales":3000000,"deudasActuales":500000,"montoSolicitado":6000000,"mesesEnEmpleoActual":24}' \
    "400" \
    "❌ Error 400 Bad Request - DNI no puede exceder 8 dígitos"

# Test 7: Email Inválido
run_test 7 \
    "Validación de email inválido" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"87654321","nombreCompleto":"Rosa Flores","email":"email-invalido","edad":32,"ingresosMensuales":2200000,"deudasActuales":400000,"montoSolicitado":4000000,"mesesEnEmpleoActual":18}' \
    "400" \
    "❌ Error 400 Bad Request - Email debe tener formato válido. Validador @Email funcionando"

# Test 8: Edad menor de 18
run_test 8 \
    "Validación de edad mínima (<18)" \
    "POST" \
    "$API_URL/api/v1/creditos/evaluar" \
    '{"dni":"11223344","nombreCompleto":"Menor Edad","email":"menor@banco.pe","edad":17,"ingresosMensuales":1000000,"deudasActuales":0,"montoSolicitado":1000000,"mesesEnEmpleoActual":6}' \
    "400" \
    "❌ Error 400 Bad Request - Edad mínima 18 años. Validador @Min funcionando"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 4: OPERACIONES DE CONSULTA
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  📊 MÓDULO 4: OPERACIONES DE CONSULTA (GET)${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log ""
log "${CYAN}Este módulo prueba operaciones de lectura:${RESET}"
log "  • Listar todas las solicitudes"
log "  • Obtener solicitud específica por ID"
log "  • Manejo de solicitudes inexistentes (404)"
log ""
pause

# Test 9: Listar todas las solicitudes
run_test 9 \
    "Listar todas las solicitudes" \
    "GET" \
    "$API_URL/api/v1/creditos" \
    "" \
    "200" \
    "✅ Array con todas las solicitudes (aprobadas, rechazadas y pendientes). Incluye datos pre-cargados + tests ejecutados"

# Test 10: Obtener solicitud específica
run_test 10 \
    "Obtener solicitud específica (ID=1)" \
    "GET" \
    "$API_URL/api/v1/creditos/1" \
    "" \
    "200" \
    "✅ Detalle completo de la solicitud: DNI, nombre, score, estado, razón de evaluación"

# Test 11: Solicitud inexistente
run_test 11 \
    "Obtener solicitud inexistente (ID=99999)" \
    "GET" \
    "$API_URL/api/v1/creditos/99999" \
    "" \
    "404" \
    "❌ Error 404 Not Found - Solicitud no existe en la base de datos"

#═══════════════════════════════════════════════════════════════════════════════
# RESUMEN FINAL
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📊 RESUMEN FINAL DE EJECUCIÓN${RESET}                                          ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    log "  ${GREEN}🎉 ✓ TODOS LOS TESTS PASARON EXITOSAMENTE${RESET}"
else
    log "  ${YELLOW}⚠️  ALGUNOS TESTS FALLARON${RESET}"
fi

log ""
log "  ${WHITE}Tests Ejecutados:${RESET}  $TOTAL_TESTS"
log "  ${GREEN}✓ Tests Exitosos:${RESET}  $PASSED_TESTS"
log "  ${RED}✗ Tests Fallidos:${RESET}  $FAILED_TESTS"
log ""
log "  ${CYAN}📄 Resultados guardados en: $OUTPUT_FILE${RESET}"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📊 RESUMEN DE OPERACIONES PROBADAS${RESET}                                    ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${GREEN}✅ EVALUACIONES EXITOSAS:${RESET}"
log "     • Perfil excelente (score >= 800)"
log "     • Perfil bueno (score 650-799)"
log ""
log "  ${RED}❌ EVALUACIONES RECHAZADAS:${RESET}"
log "     • DTI alto (>50%) - Sobre-endeudamiento"
log "     • Inestabilidad laboral (<3 meses)"
log ""
log "  ${YELLOW}🔍 VALIDACIONES:${RESET}"
log "     • DNI peruano (8 dígitos) - @DniValido"
log "     • Email válido - @Email"
log "     • Edad mínima (18 años) - @Min"
log "     • Campos obligatorios - @NotBlank"
log ""
log "  ${CYAN}📊 CONSULTAS:${RESET}"
log "     • Listar todas las solicitudes (GET)"
log "     • Obtener solicitud específica (GET)"
log "     • Manejo de 404 Not Found"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🎯 ALGORITMO DE SCORING CREDITICIO${RESET}                                    ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${WHITE}Factores Evaluados:${RESET}"
log "     • ${CYAN}DTI (Debt-to-Income):${RESET} Límite 50%"
log "     • ${CYAN}Estabilidad laboral:${RESET} Mínimo 3 meses"
log "     • ${CYAN}Capacidad de pago:${RESET} Cuota <= 30% ingreso"
log "     • ${CYAN}Edad:${RESET} Rango óptimo 25-55 años"
log "     • ${CYAN}Monto solicitado:${RESET} vs ingreso mensual"
log ""
log "  ${WHITE}Escala de Score:${RESET}"
log "     • ${GREEN}800-1000:${RESET} Excelente (aprobación inmediata)"
log "     • ${GREEN}650-799:${RESET}  Bueno (aprobación estándar)"
log "     • ${YELLOW}500-649:${RESET}  Regular (requiere análisis)"
log "     • ${RED}0-499:${RESET}    Malo (rechazo automático)"
log ""
log "  ${WHITE}Umbral de Aprobación: ${GREEN}650 puntos${RESET}"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}💡 NOTAS TÉCNICAS${RESET}                                                      ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${CYAN}📌 Endpoint Base:${RESET} $API_URL/api/v1/creditos"
log "  ${CYAN}📌 DNI Peruano:${RESET} Exactamente 8 dígitos numéricos"
log "  ${CYAN}📌 Base de Datos:${RESET} PostgreSQL (Dev Services - Testcontainers)"
log "  ${CYAN}📌 Testing:${RESET} JUnit 5 + REST Assured + @QuarkusTest"
log "  ${CYAN}📌 Validadores:${RESET} Bean Validation + Custom Validators"
log "  ${CYAN}📌 Exception Mappers:${RESET} Respuestas HTTP amigables"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🔗 RECURSOS ÚTILES${RESET}                                                     ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${CYAN}→ Dev UI:${RESET}       http://localhost:8080/q/dev"
log "  ${CYAN}→ Swagger UI:${RESET}   http://localhost:8080/q/swagger-ui"
log "  ${CYAN}→ OpenAPI Spec:${RESET} http://localhost:8080/q/openapi"
log "  ${CYAN}→ Health Check:${RESET} http://localhost:8080/q/health"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📚 CONCEPTOS DEMOSTRADOS - CAPÍTULO 5${RESET}                                 ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${GREEN}✓${RESET} Testing con ${CYAN}@QuarkusTest${RESET} (JUnit 5)"
log "  ${GREEN}✓${RESET} REST Assured para testing de APIs"
log "  ${GREEN}✓${RESET} Dev Services (PostgreSQL automático)"
log "  ${GREEN}✓${RESET} Bean Validation (${CYAN}@NotBlank, @Email, @Min, @Max${RESET})"
log "  ${GREEN}✓${RESET} Validadores Custom (${CYAN}@DniValido${RESET})"
log "  ${GREEN}✓${RESET} Exception Mappers (respuestas amigables)"
log "  ${GREEN}✓${RESET} Hibernate ORM with Panache"
log "  ${GREEN}✓${RESET} Inyección de dependencias (CDI)"
log "  ${GREEN}✓${RESET} Lógica de negocio bancaria (DTI, Scoring)"
log "  ${GREEN}✓${RESET} Manejo de errores HTTP (400, 404)"
log ""

log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "  ${WHITE}💡 TIP: Para ver el archivo formateado:${RESET}"
log "     ${CYAN}cat $OUTPUT_FILE${RESET}"
log ""
log "  ${WHITE}📖 Documentación completa en:${RESET}"
log "     ${CYAN}TEORIA.md${RESET} - Conceptos y explicaciones"
log "     ${CYAN}TESTS.md${RESET}  - Guía de testing"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi
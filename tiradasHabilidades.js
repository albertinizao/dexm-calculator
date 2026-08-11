(function () {
    var nombresAtributos = {
        "astronavegar": "astronavegar",
        "atractivo": "atractivo",
        "buscar": "buscar",
        "conduccion": "conduccion",
        "cruzarbifrost": "cruzarbifrost",
        "deporte": "deporte",
        "destreza": "destreza",
        "diplomacia": "diplomacia",
        "einherjer": "einherjer",
        "engañar": "engano",
        "enganno": "engano",
        "esconderse": "esconderse",
        "evolucioncurva": "evolcurva",
        "evoluccioncurva": "evolcurva",
        "evolutivocurva": "evolcurva",
        "esquiva": "esquiva",
        "fisicaquimica": "fisicaquimica",
        "fuerza": "fuerza",
        "informatica": "informatica",
        "intimidar": "intimidar",
        "labia": "labia",
        "liderazgo": "liderazgo",
        "medicina": "medicina",
        "provocar": "provocar",
        "punteria": "punteria",
        "resistencia": "resistencia",
        "sentiryggdrasil": "sentiryggdrasil"
    };

    function normalizar(texto) {
        return (texto || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]/g, "");
    }

    function obtenerPrueba(habilidad) {
        var prueba = habilidad && habilidad.Prueba ? String(habilidad.Prueba) : "";
        var coincidencia = prueba.match(/^(.+?)\s+(\d+)\+\s*$/);
        if (!coincidencia) {
            return { nombre: prueba.replace(/\s+\*\s*$/, "").trim(), dificultad: null };
        }
        return { nombre: coincidencia[1].trim(), dificultad: parseInt(coincidencia[2], 10) };
    }

    function obtenerIdAtributo(nombre) {
        var clave = normalizar(nombre).replace("fisicaquimica", "fisicaquimica");
        return nombresAtributos[clave] || clave;
    }

    function numeroDeElemento(id, defecto) {
        var elemento = document.getElementById(id);
        var valor = elemento && ("value" in elemento ? elemento.value : elemento.textContent);
        return valor !== "" && !isNaN(valor) ? Number(valor) : defecto;
    }

    function obtenerContextoAtributo(nombre, usarNuevo) {
        var id = obtenerIdAtributo(nombre);
        var sufijo = usarNuevo ? "New" : "Actual";
        var total = numeroDeElemento(id + sufijo + "Total", NaN);
        if (isNaN(total)) total = numeroDeElemento(id + "ActualTotal", 0);
        return {
            id: id,
            total: total,
            plusOne: numeroDeElemento(id + sufijo + "+1", 0),
            plusD6: Math.max(0, Math.floor(numeroDeElemento(id + sufijo + "+D6", 0)))
        };
    }

    function dado(lados) {
        return Math.floor(Math.random() * lados) + 1;
    }

    function prepararDadosD6(cantidad) {
        var dados = [];
        for (var i = 0; i < cantidad; i++) {
            dados.push({ id: i, valor: dado(6), desactivado: false, seleccionado: false });
        }
        var noUnos = dados.filter(function (die) { return die.valor !== 1; });
        var candidatos = noUnos.length ? noUnos : dados;
        candidatos.sort(function (a, b) { return b.valor - a.valor || a.id - b.id; });
        if (candidatos[0]) candidatos[0].desactivado = true;
        var disponibles = dados.filter(function (die) { return !die.desactivado; });
        if (disponibles.length >= 2) {
            disponibles.sort(function (a, b) { return b.valor - a.valor || a.id - b.id; });
            disponibles.slice(0, 2).forEach(function (die) { die.seleccionado = true; });
        }
        return dados;
    }

    function imagenD10(nombre, habilidad, usarNuevo) {
        var boton = document.createElement("button");
        boton.type = "button";
        boton.className = "tiradaHabilidadBoton";
        boton.title = "Tirar la prueba de " + (nombre || "habilidad");
        boton.setAttribute("aria-label", "Tirar la prueba de " + (nombre || "habilidad"));
        var imagen = document.createElement("img");
        imagen.src = "./diceD10.png";
        imagen.alt = "Tirar D10";
        boton.appendChild(imagen);
        boton.onclick = function (evento) {
            evento.stopPropagation();
            mostrarTiradaHabilidad(habilidad, usarNuevo);
        };
        return boton;
    }

    function mostrarTiradaHabilidad(habilidad, usarNuevo) {
        var prueba = obtenerPrueba(habilidad);
        var contexto = obtenerContextoAtributo(prueba.nombre, usarNuevo);
        var d10 = dado(10);
        var dadosD6 = prepararDadosD6(2 + contexto.plusD6);
        var seleccionados = dadosD6.filter(function (die) { return die.seleccionado; });
        var suma = d10 + seleccionados.reduce(function (total, die) { return total + die.valor; }, 0) + contexto.plusOne;
        var exito = prueba.dificultad !== null && suma >= prueba.dificultad;
        var pifia = suma <= 3;
        var resultado = pifia ? "PIFIA" : prueba.dificultad === null ? "Sin dificultad numérica de activación" : (exito ? "ÉXITO: habilidad activada" : "Fallo");
        var detalleD6 = seleccionados.length ? seleccionados.map(function (die) { return "D6 " + die.valor; }).join(" + ") : "sin D6 seleccionables";
        var estadoD6 = dadosD6.map(function (die) {
            return "D6 " + die.valor + (die.desactivado ? " (desactivado)" : die.seleccionado ? " (seleccionado)" : "");
        }).join(" · ");
        var detalle = "Tirada: D10 " + d10 + " + " + detalleD6 + (contexto.plusOne ? " + " + contexto.plusOne : "") + " = " + suma + "<br><small>" + estadoD6 + "</small>";
        var dialogo = document.getElementById("dialogoTiradaHabilidad");
        if (!dialogo) {
            dialogo = document.createElement("dialog");
            dialogo.id = "dialogoTiradaHabilidad";
            dialogo.className = "dialogoTiradaHabilidad";
            document.body.appendChild(dialogo);
        }
        dialogo.innerHTML = "<h2>Prueba de " + (prueba.nombre || "habilidad") + "</h2>" +
            "<p>" + detalle + "</p>" +
            (prueba.dificultad === null ? "<p>No hay una dificultad numérica en esta habilidad.</p>" : "<p>Dificultad: " + prueba.dificultad + "+</p>") +
            "<p class=\"resultadoTiradaHabilidad " + (pifia ? "pifia" : exito ? "exito" : "fallo") + "\"><strong>" + resultado + "</strong></p>" +
            "<button type=\"button\" id=\"cerrarTiradaHabilidad\">Cerrar</button>";
        document.getElementById("cerrarTiradaHabilidad").onclick = function () { dialogo.close(); };
        if (typeof dialogo.showModal === "function") dialogo.showModal();
        else dialogo.setAttribute("open", "open");
    }

    window.crearImagenD10Habilidad = imagenD10;
    window.mostrarTiradaHabilidad = mostrarTiradaHabilidad;
}());

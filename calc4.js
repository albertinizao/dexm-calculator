  var  heroe=0; var  norna=0; var  alfar=0; var  valkiria=0; var  risa=0; var  dvergr=0; var  fisico=0; var  agilidad=0; var  percepcion=0; var 
                                    mente=0; var  estudio=0; var  carisma=0; var astronavegar=0; var  atractivo=0; var  buscar=0; var  conduccion=0; var  cruzar=0; var  deporte=0; var  destreza=0; var 
                                    diplomacia=0; var  einherjer=0; var  enganno=0; var  esconderse=0; var  evol=0; var  esquiva=0; var  fisQuim=0; var  fuerza=0; var  
                                    informatica=0; var  intimidar=0; var  labia=0; var  liderazgo=0; var  medicina=0; var  provocar=0; var  punteria=0; var  resistencia=0;  
                                    var  sentir=0;

                  
            function obtenerHabilidadesActual(){
                limpiarHabilidades();
                document.getElementById("TituloListadoHabildiades").innerHTML="Todas las habilidades aprendidas hasta ahora";

                fetch('https://albertinizao.github.io/dexm-calculator/CompendioHabilidadesExport.json')
                    .then((response) => response.json())
                    .then((json) => json.forEach((element) => {
                        aprenderHabilidadNew(element.Nombre, element.Unica, element);
                    }));
            }
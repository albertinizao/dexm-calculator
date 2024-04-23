
                        
            function aprenderHabilidadActual(nombre, unica, habilidad){
                if (comprobarHabilidadActual(habilidad)){
                    var div = document.createElement("div");
                    var p = document.createElement("p");
                    p.className="abilityName";
                    div.onclick = function () {
                        var ChatWindow_Height = 650;
                        var ChatWindow_Width = 570;
                        window.open("habilidad.html?ability="+hashCode(nombre), nombre, "height=" + ChatWindow_Height + ", width = " + ChatWindow_Width);
                    };
                    if (unica=="Sí"){
                        p.innerHTML="["+nombre+"] (Única)"
                    }else{
                        p.innerHTML=nombre
                    }
                    div.className="valign";
                    div.appendChild(p)
                    document.getElementById("NuevoListado").appendChild(div);
                    cantidad++;
                    if(cantidad==20){
                        document.getElementById("NuevoListado").className="listadoHab2";
                    }
                    if(cantidad==30){
                        document.getElementById("NuevoListado").className="listadoHab3";
                    }
                    if(cantidad==40){
                        document.getElementById("NuevoListado").className="listadoHab4";
                    }
                    if(cantidad==50){
                        document.getElementById("NuevoListado").className="listadoHab5";
                    }
                }
            }
            
            function comprobarHabilidadActual(habilidad){
                if (comprobarGenetica(habilidad.Heroe, habilidad.Norna, habilidad.Alfar, habilidad.Valkiria, habilidad.Risa, habilidad.Dvergr)){
                    if (comprobarPrincipal(habilidad.Fis, habilidad.Agi, habilidad.Pcn, habilidad.Mnt, habilidad.Est, habilidad.Car)){
                        if (comprobarSecundarias(habilidad.Astronavegar, habilidad.Atractivo, habilidad.Buscar, habilidad.Conduccion, habilidad.CruzarBifrost, 
                                habilidad.Deporte, habilidad.Destreza, habilidad.Diplomacia, habilidad.Einherjer, habilidad.Enganno, habilidad.Esconderse, 
                                habilidad.EvoluccionCurva, habilidad.Esquiva, habilidad.FisicaQuimica, habilidad.Fuerza, habilidad.Informatica, habilidad.Intimidar,
                                habilidad.Labia, habilidad.Liderazgo, habilidad.Medicina, habilidad.Provocar, habilidad.Punteria, habilidad.Resistencia, habilidad.SentirYggdrasil)){
                            return true;
                        }
                    }
                }
                return false;
            }
            
            
            function comprobarGeneticaActual(heroe, norna, alfar, valkiria, risa, dvergr){
                if (comprobarAtributoGeneticaActual("heroe",heroe)){
                    if (comprobarAtributoGeneticaActual("norna",norna)){
                        if (comprobarAtributoGeneticaActual("alfar",alfar)){
                            if (comprobarAtributoGeneticaActual("valkiria",valkiria)){
                                if (comprobarAtributoGeneticaActual("risa",risa)){
                                    if (comprobarAtributoGeneticaActual("dvergr",dvergr)){
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }
            
            function comprobarPrincipalActual(fisico, agilidad, percepcion, mente, estudio, carisma){
                if (comprobarAtributoActual("fisico", fisico)){
                    if (comprobarAtributoActual("agilidad", agilidad)){
                        if (comprobarAtributoActual("percepcion", percepcion)){
                            if (comprobarAtributoActual("mente", mente)){
                                if (comprobarAtributoActual("estudio", estudio)){
                                    if (comprobarAtributoActual("carisma", carisma)){
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }
            
            function comprobarSecundariasActual(astronavegar, atractivo, buscar, conduccion, cruzar, deporte, destreza, diplomacia, einherjer, enganno, esconderse,
                    evol, esquiva, fisQuim, fuerza,informatica, intimidar, labia, liderazgo, medicina, provocar, punteria, resistencia, sentir){
                if (comprobarAtributoActual("astronavegar", astronavegar)){
                  if (comprobarAtributoActual("atractivo", atractivo)){
                      if (comprobarAtributoActual("buscar", buscar)){
                          if (comprobarAtributoActual("conduccion", conduccion)){
                              if (comprobarAtributoActual("cruzarbifrost", cruzar)){
                                  if (comprobarAtributoActual("deporte", deporte)){
                                      if (comprobarAtributoActual("destreza", destreza)){
                                          if (comprobarAtributoActual("diplomacia", diplomacia)){
                                              if (comprobarAtributoActual("einherjer", einherjer)){
                                                  if (comprobarAtributoActual("engano", enganno)){
                                                      if (comprobarAtributoActual("esconderse", esconderse)){
                                                          if (comprobarAtributoActual("evolcurva", evol)){
                                                              if (comprobarAtributoActual("esquiva", esquiva)){
                                                                  if (comprobarAtributoActual("fisicaquimica", fisQuim)){
                                                                      if (comprobarAtributoActual("fuerza", fuerza)){
                                                                          if (comprobarAtributoActual("informatica", informatica)){
                                                                              if (comprobarAtributoActual("intimidar", intimidar)){
                                                                                  if (comprobarAtributoActual("labia", labia)){
                                                                                      if (comprobarAtributoActual("liderazgo", liderazgo)){
                                                                                          if (comprobarAtributoActual("medicina", medicina)){
                                                                                              if (comprobarAtributoActual("provocar", provocar)){
                                                                                                  if (comprobarAtributoActual("punteria", punteria)){
                                                                                                      if (comprobarAtributoActual("resistencia", resistencia)){
                                                                                                          if (comprobarAtributoActual("sentiryggdrasil", sentir)){
                                                                                                              return true;
                                                                                                          }
                                                                                                      }
                                                                                                  }
                                                                                              }
                                                                                          }
                                                                                      }
                                                                                  }
                                                                              }
                                                                          }
                                                                      }
                                                                  }
                                                              }
                                                          }
                                                      }
                                                  }
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                      }
                  }
                }
                return false;
            }
            
            function comprobarAtributoGeneticaActual(nombreAtributo, valor){
                return (isNaN(valor) || valor==0 || valor<=document.getElementById(nombreAtributo+"Actual").value || valor<=document.getElementById(nombreAtributo+"Actual").innerHTML);
            }
            
            function comprobarAtributoActual(nombreAtributo, valor){
                return (isNaN(valor) || valor==0 || valor<=document.getElementById(nombreAtributo+"ActualTotal").innerHTML*1);
            }
            
            function aprenderHabilidadNew(nombre, unica, habilidad){
                if (comprobarHabilidad(habilidad)){
                    var div = document.createElement("div");
                    var p = document.createElement("p");
                    p.className="abilityName";
                    div.onclick = function () {
                        var ChatWindow_Height = 650;
                        var ChatWindow_Width = 570;
                        window.open("habilidad.html?ability="+hashCode(nombre), nombre, "height=" + ChatWindow_Height + ", width = " + ChatWindow_Width);
                    };
                    if (unica=="Sí"){
                        p.innerHTML="["+nombre+"] (Única)"
                    }else{
                        p.innerHTML=nombre
                    }
                    div.className="valign";
                    div.appendChild(p)
                    document.getElementById("NuevoListado").appendChild(div);
                    cantidad++;
                    if(cantidad==20){
                        document.getElementById("NuevoListado").className="listadoHab2";
                    }
                    if(cantidad==30){
                        document.getElementById("NuevoListado").className="listadoHab3";
                    }
                    if(cantidad==40){
                        document.getElementById("NuevoListado").className="listadoHab4";
                    }
                    if(cantidad==50){
                        document.getElementById("NuevoListado").className="listadoHab5";
                    }
                }
            }
            
            function comprobarHabilidad(habilidad){
                if (comprobarGenetica(habilidad.Heroe, habilidad.Norna, habilidad.Alfar, habilidad.Valkiria, habilidad.Risa, habilidad.Dvergr)){
                    if (comprobarPrincipal(habilidad.Fis, habilidad.Agi, habilidad.Pcn, habilidad.Mnt, habilidad.Est, habilidad.Car)){
                        if (comprobarSecundarias(habilidad.Astronavegar, habilidad.Atractivo, habilidad.Buscar, habilidad.Conduccion, habilidad.CruzarBifrost, 
                                habilidad.Deporte, habilidad.Destreza, habilidad.Diplomacia, habilidad.Einherjer, habilidad.Enganno, habilidad.Esconderse, 
                                habilidad.EvolutivoCurva, habilidad.Esquiva, habilidad.FisicaQuimica, habilidad.Fuerza, habilidad.Informatica, habilidad.Intimidar,
                                habilidad.Labia, habilidad.Liderazgo, habilidad.Medicina, habilidad.Provocar, habilidad.Punteria, habilidad.Resistencia, habilidad.SentirYggdrasil)){
                            return true;
                        }
                    }
                }
                return false;
            }
            
            
            function comprobarGenetica(heroe, norna, alfar, valkiria, risa, dvergr){
                if (comprobarAtributoGenetica("heroe",heroe)){
                    if (comprobarAtributoGenetica("norna",norna)){
                        if (comprobarAtributoGenetica("alfar",alfar)){
                            if (comprobarAtributoGenetica("valkiria",valkiria)){
                                if (comprobarAtributoGenetica("risa",risa)){
                                    if (comprobarAtributoGenetica("dvergr",dvergr)){
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }
            
            function comprobarPrincipal(fisico, agilidad, percepcion, mente, estudio, carisma){
                if (comprobarAtributo("fisico", fisico)){
                    if (comprobarAtributo("agilidad", agilidad)){
                        if (comprobarAtributo("percepcion", percepcion)){
                            if (comprobarAtributo("mente", mente)){
                                if (comprobarAtributo("estudio", estudio)){
                                    if (comprobarAtributo("carisma", carisma)){
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }
            
            function comprobarSecundarias(astronavegar, atractivo, buscar, conduccion, cruzar, deporte, destreza, diplomacia, einherjer, enganno, esconderse,
                    evol, esquiva, fisQuim, fuerza,informatica, intimidar, labia, liderazgo, medicina, provocar, punteria, resistencia, sentir){
                        var entero=0;
                if (comprobarAtributo("astronavegar", astronavegar)){
                  if (comprobarAtributo("atractivo", atractivo)){
                      if (comprobarAtributo("buscar", buscar)){
                          if (comprobarAtributo("conduccion", conduccion)){
                              if (comprobarAtributo("cruzarbifrost", cruzar)){
                                  if (comprobarAtributo("deporte", deporte)){
                                      if (comprobarAtributo("destreza", destreza)){
                                          if (comprobarAtributo("diplomacia", diplomacia)){
                                              if (comprobarAtributo("einherjer", einherjer)){
                                                  if (comprobarAtributo("engano", enganno)){
                                                      if (comprobarAtributo("esconderse", esconderse)){
                                                          if (comprobarAtributo("evolcurva", evol)){
                                                              if (comprobarAtributo("esquiva", esquiva)){
                                                                  if (comprobarAtributo("fisicaquimica", fisQuim)){
                                                                      if (comprobarAtributo("fuerza", fuerza)){
                                                                          if (comprobarAtributo("informatica", informatica)){
                                                                              if (comprobarAtributo("intimidar", intimidar)){
                                                                                  if (comprobarAtributo("labia", labia)){
                                                                                      if (comprobarAtributo("liderazgo", liderazgo)){
                                                                                          if (comprobarAtributo("medicina", medicina)){
                                                                                              if (comprobarAtributo("provocar", provocar)){
                                                                                                  if (comprobarAtributo("punteria", punteria)){
                                                                                                      if (comprobarAtributo("resistencia", resistencia)){
                                                                                                          if (comprobarAtributo("sentiryggdrasil", sentir)){
                                                                                                              return true;
                                                                                                          }
                                                                                                      }
                                                                                                  }
                                                                                              }
                                                                                          }
                                                                                      }
                                                                                  }
                                                                              }
                                                                          }
                                                                      }
                                                                  }
                                                              }
                                                          }
                                                      }
                                                  }
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                      }
                  }
                }
                return false;
            }
            
            function comprobarAtributo(nombreAtributo, valor){
                return (isNaN(valor) || valor==0 || valor<=document.getElementById(nombreAtributo+"NewTotal").innerHTML*1);
            }
            
            function comprobarAtributoGenetica(nombreAtributo, valor){
                return (isNaN(valor) || valor==0 || valor<=document.getElementById(nombreAtributo+"Total").innerHTML*1);
            }
            
            var cantidad=0;
            
            function aprenderHabilidadComprobar(nombre, unica, habilidad){
                if (comprobarHabilidad(habilidad)){
                  if(!comprobarHabilidadActual(habilidad)){
                        var p = document.createElement("p");
                        p.onclick = function () {
                            var ChatWindow_Height = 650;
                            var ChatWindow_Width = 570;
                            window.open("habilidad.html?ability="+hashCode(nombre), nombre, "height=" + ChatWindow_Height + ", width = " + ChatWindow_Width);
                        };
                        if (nombre==="Alfheim"){
                            alert();
                        }
                        if (unica=="Sí"){
                            p.innerHTML="["+nombre+"]"
                        }else{
                            p.innerHTML=nombre
                        }
                        cantidad++;
                        if(cantidad==20){
                            document.getElementById("NuevoListado").className="listadoHab2";
                        }
                        if(cantidad==30){
                            document.getElementById("NuevoListado").className="listadoHab3";
                        }
                        if(cantidad==40){
                            document.getElementById("NuevoListado").className="listadoHab4";
                        }
                        if(cantidad==50){
                            document.getElementById("NuevoListado").className="listadoHab5";
                        }
                        document.getElementById("NuevoListado").appendChild(p);
                    }
                }
            }
            function limpiarHabilidades(){
                document.getElementById("TituloListadoHabildiades").innerHTML=" ";
                document.getElementById("NuevoListado").innerHTML=" ";
                cantidad=0;
                document.getElementById("NuevoListado").className="listadoHab1";
            }
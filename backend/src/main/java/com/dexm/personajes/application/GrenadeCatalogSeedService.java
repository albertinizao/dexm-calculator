package com.dexm.personajes.application;

import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogEntity;
import com.dexm.personajes.adapter.out.persistence.GrenadeCatalogRepository;
import java.util.*;

/** Compatibility tool for historical/manual migrations. It is never invoked at startup or by the UI. */
public class GrenadeCatalogSeedService {
    private final GrenadeCatalogRepository catalog;
    public GrenadeCatalogSeedService(GrenadeCatalogRepository catalog) { this.catalog = catalog; }
    public void seedIfMissing() {
        Map<String, GrenadeCatalogEntity> existing = new LinkedHashMap<>(); catalog.findAll().forEach(e -> existing.put(e.getId(), e));
        for (Seed s : seeds()) {
            var e=existing.get(s.id);
            if (e == null && s.id.equals("fragmentacion-estandar")) {
                var legacy = existing.get("basic-grenade");
                if (legacy != null && legacy.isOfficial()) catalog.delete(legacy);
            }
            if(e==null) catalog.save(s.entity());
            else { e.setName(s.name); e.setCentralDamage(s.c); e.setAdjacentDamage(s.a); e.setDamageDecay(s.d); e.setAdditionalEffect(s.effect); e.setHandGrenade(s.hand); e.setType(s.type); e.setOfficial(true); catalog.save(e); }
        }
    }
    private static List<Seed> seeds(){return List.of(
        s("fragmentacion-estandar","Fragmentación estándar",400,100,20,"—",true,null),s("conmocion-mano","Conmoción",300,120,40,"Quien esté en centro o adyacentes hace Resistencia 14. Si falla, tumbado. Si falla por 5+, pierde 1 acción siguiente turno.",true,null),s("aturdidora-mano","Aturdidora",0,0,0,"Resistencia 19/17/15/13 a distancia 0/1/2/3. Fallo 1-4 pierde 1 acción; 5-8 pierde 2; 9+ pierde 2 y 1 acción siguiente turno.",true,null),s("humo-mano","Humo",0,0,0,"Humo radio 3 durante 4 asaltos. Línea de tiro que atraviese humo Puntería -4. Si atraviesa 3+ casillas, no puede atacarse por visión directa.",true,null),s("gas-lacrimogeno-mano","Gas lacrimógeno",0,0,0,"Nube radio 3 durante 5 asaltos. Al comenzar turno dentro: Resistencia 14. Fallo: -4 a todas las tiradas y pierde 1 acción; fallo 5+: pierde 2 acciones. Protección respiratoria y ocular adecuada: inmune.",true,null),s("incendiaria-mano","Incendiaria",120,70,20,"Centro y adyacentes arden 3 asaltos. Entrar/comenzar causa 30 daño. Personaje con daño inicial queda en llamas: 30 al final turno hasta gastar 1 acción apagarse.",true,null),s("he-fragmentacion-lg","HE Fragmentación",300,80,20,"—",false,"LG"),s("hedp-lg","HEDP",250,60,20,"Impacto directo contra personaje/vehículo/estructura: RD/Armadura mitad frente daño central. Explosión normal.",false,"LG"),s("conmocion-lg","Conmoción",250,100,40,"Centro/adyacentes Resistencia 14. Fallo tumbado. Fallo 5+ tumbado y pierde 1 acción.",false,"LG"),s("aturdidora-lg","Aturdidora",0,0,0,"igual Aturdidora mano.",false,"LG"),s("humo-lg","Humo",0,0,0,"igual Humo mano.",false,"LG"),s("gas-lacrimogeno-lg","Gas lacrimógeno",0,0,0,"igual Gas mano.",false,"LG"),s("incendiaria-lg","Incendiaria",100,60,20,"Centro/adyacentes arden 3 asaltos; casilla incendiada 30 al entrar/comenzar; personaje incendiado 30 final turno hasta gastar 1 acción.",false,"LG"),s("iluminacion-lg","Iluminación",0,0,0,"Ilumina radio 20 durante 5 asaltos; ignora penalizadores derivados solo de oscuridad.",false,"LG"),s("he-fragmentacion-pesada","HE Fragmentación pesada",400,120,20,"—",false,"LP"),s("hedp-pesada","HEDP pesada",350,80,20,"Impacto directo RD/Armadura mitad frente daño central; explosión normal.",false,"LP"),s("airburst","Airburst",350,100,20,"Ignora cobertura que no proteja también desde arriba. Cobertura completamente cerrada normal.",false,"LP"),s("humo-pesado","Humo pesado",0,0,0,"Humo radio 5 durante 5 asaltos; línea Puntería -4; 3+ casillas sin visión directa.",false,"LP"),s("incendiaria-pesada","Incendiaria pesada",150,100,20,"Casillas hasta distancia 2 arden 4 asaltos; entrar/comenzar 40 daño; incendiado 40 final turno hasta gastar 1 acción.",false,"LP"),s("iluminacion-pesada","Iluminación pesada",0,0,0,"Radio 40 durante 8 asaltos; elimina penalizadores solo por oscuridad.",false,"LP"));}
    private static Seed s(String id,String name,int c,int a,int d,String effect,boolean hand,String type){return new Seed(id,name,c,a,d,effect,hand,type);}
    private record Seed(String id,String name,int c,int a,int d,String effect,boolean hand,String type){GrenadeCatalogEntity entity(){return new GrenadeCatalogEntity(id,name,null,c,a,d,effect,hand,type,true);}}
}

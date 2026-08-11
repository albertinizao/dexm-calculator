package com.dexm.personajes;

import com.dexm.personajes.adapter.in.web.CharacterController.WeaponRequest;
import com.dexm.personajes.adapter.out.persistence.*;
import com.dexm.personajes.application.WeaponInventoryService;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WeaponInventoryServiceTest {
    CharacterRepository characters; WeaponRepository weapons; WeaponInventoryService service;
    @BeforeEach void setUp(){characters=mock(CharacterRepository.class);weapons=mock(WeaponRepository.class);service=new WeaponInventoryService(characters,weapons);when(characters.existsById("c1")).thenReturn(true);}
    WeaponRequest request(String slot,String size){return request(slot,size,bd(0));}
    WeaponRequest request(String slot,String size,BigDecimal loadedBullets){return new WeaponRequest(slot,"Arma","PISTOLA",size,bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),loadedBullets,"9mm",null);}
    BigDecimal bd(int n){return BigDecimal.valueOf(n);}
    WeaponEntity entity(String id,String slot,String size){return new WeaponEntity(id,"c1",slot,"Arma","PISTOLA",size,bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),"9mm",null);}
    WeaponEntity shootingEntity(String id, String rate, String automaticFire, int loadedBullets) {
        var entity = new WeaponEntity(id,"c1","SMALL_1","Arma","PISTOLA","PEQUENA",bd(10),bd(1),rate,
                bd(4),bd(3),bd(2),bd(1),null,automaticFire,bd(12),bd(loadedBullets),"9mm",null);
        return entity;
    }
    @Test void createsAndReplacesSlot(){when(weapons.save(any())).thenAnswer(i->i.getArgument(0));when(weapons.findByCharacterIdAndSlot("c1","SMALL_1")).thenReturn(Optional.empty());var result=service.create("c1",request("SMALL_1","PEQUENA"));assertThat(result).containsEntry("slot","SMALL_1");verify(weapons).save(any());}
    @Test void persistsAndReturnsLoadedBullets(){when(weapons.save(any())).thenAnswer(i->i.getArgument(0));when(weapons.findByCharacterIdAndSlot("c1","SMALL_1")).thenReturn(Optional.empty());var result=service.create("c1",request("SMALL_1","PEQUENA",bd(3)));assertThat(result).containsEntry("loadedBullets",bd(3));}
    @Test void rejectsNegativeLoadedBullets(){assertThatThrownBy(()->service.create("c1",request("SMALL_1","PEQUENA",bd(-1)))).hasMessageContaining("entre 0");}
    @Test void rejectsFractionalLoadedBullets(){assertThatThrownBy(()->service.create("c1",request("SMALL_1","PEQUENA",new BigDecimal("1.5")))).hasMessageContaining("entero");}
    @Test void rejectsLoadedBulletsAboveCapacity(){assertThatThrownBy(()->service.create("c1",request("SMALL_1","PEQUENA",bd(9)))).hasMessageContaining("capacidad");}
    @Test void updatesAndReturnsLoadedBullets(){var source=entity("a","SMALL_1","PEQUENA");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));when(weapons.save(any())).thenAnswer(i->i.getArgument(0));var result=service.update("c1","a",request("SMALL_1","PEQUENA",bd(4)));assertThat(source.getLoadedBullets()).isEqualByComparingTo(bd(4));assertThat(result).containsEntry("loadedBullets",bd(4));}
    @Test void rejectsIncompatibleSize(){assertThatThrownBy(()->service.create("c1",request("MEDIUM_1","GRANDE"))).hasMessageContaining("no cabe");}
    @Test void swapsCompatibleWeapons(){var source=entity("a","SMALL_1","PEQUENA");var target=entity("b","MEDIUM_1","PEQUENA");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));when(weapons.findByCharacterIdAndSlot("c1","MEDIUM_1")).thenReturn(Optional.of(target));when(weapons.save(any())).thenAnswer(i->i.getArgument(0));service.move("c1","a","MEDIUM_1");assertThat(source.getSlot()).isEqualTo("MEDIUM_1");assertThat(target.getSlot()).isEqualTo("SMALL_1");}
    @Test void rejectsSwapWhenTargetDoesNotFitSourceSlot(){var source=entity("a","ANY","GRANDE");var target=entity("b","SMALL_1","PEQUENA");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));when(weapons.findByCharacterIdAndSlot("c1","SMALL_1")).thenReturn(Optional.of(target));assertThatThrownBy(()->service.move("c1","a","SMALL_1")).hasMessageContaining("no cabe");}
    @Test void preventsEditingToSizeThatNoLongerFitsItsSlot(){var source=entity("a","SMALL_1","PEQUENA");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));assertThatThrownBy(()->service.update("c1","a",request("ANY","GRANDE"))).hasMessageContaining("no cabe");}
    @Test void preventsChangingTypeOfCatalogWeapon(){var source=new WeaponEntity("a","c1","SMALL_1","Beretta 92","PISTOLA","PEQUENA",bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),"9mm",null,"catalog-1","/weapons/beretta.jpg");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));var changed=new WeaponRequest("SMALL_1","Beretta 92","SUBFUSIL","PEQUENA",bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),bd(0),"9mm",null);assertThatThrownBy(()->service.update("c1","a",changed)).hasMessageContaining("tipo");}
    @Test void shootsRequestedNormalShotsAndSubtractsLoadedBullets(){
        var source=shootingEntity("a","2x4",null,5);
        when(weapons.findByIdAndCharacterIdForUpdate("a","c1")).thenReturn(Optional.of(source));
        when(weapons.save(any())).thenAnswer(i->i.getArgument(0));

        var result=service.shoot("c1","a",new WeaponInventoryService.ShootCommand("normal",2));

        assertThat(result).containsEntry("weaponId","a").containsEntry("mode","normal")
                .containsEntry("requested",2).containsEntry("consumed",2).containsEntry("remaining",3)
                .containsEntry("cadence",2).containsEntry("automaticFire",null);
        assertThat(source.getLoadedBullets()).isEqualByComparingTo(bd(3));
        verify(weapons).save(source);
    }
    @Test void shootsAutomaticCadenceTimesAutomaticFire(){
        var source=shootingEntity("a","2x4","3",10);
        when(weapons.findByIdAndCharacterIdForUpdate("a","c1")).thenReturn(Optional.of(source));
        when(weapons.save(any())).thenAnswer(i->i.getArgument(0));

        var result=service.shoot("c1","a",new WeaponInventoryService.ShootCommand("automatic",null));

        assertThat(result).containsEntry("mode","automatic").containsEntry("requested",6)
                .containsEntry("consumed",6).containsEntry("remaining",4).containsEntry("cadence",2)
                .containsEntry("automaticFire",3);
        assertThat(source.getLoadedBullets()).isEqualByComparingTo(bd(4));
    }
    @Test void rejectsShootWhenLoadedBulletsAreInsufficient(){
        var source=shootingEntity("a","2",null,1);
        when(weapons.findByIdAndCharacterIdForUpdate("a","c1")).thenReturn(Optional.of(source));

        assertThatThrownBy(()->service.shoot("c1","a",new WeaponInventoryService.ShootCommand("normal",2)))
                .hasMessageContaining("suficientes");
        verify(weapons,never()).save(any());
        assertThat(source.getLoadedBullets()).isEqualByComparingTo(bd(1));
    }
    @Test void rejectsNonPositiveShootCount(){
        var source=shootingEntity("a","2",null,5);
        when(weapons.findByIdAndCharacterIdForUpdate("a","c1")).thenReturn(Optional.of(source));

        assertThatThrownBy(()->service.shoot("c1","a",new WeaponInventoryService.ShootCommand("normal",0)))
                .hasMessageContaining("positiva");
        assertThatThrownBy(()->service.shoot("c1","a",new WeaponInventoryService.ShootCommand("normal",-1)))
                .hasMessageContaining("positiva");
    }
    @Test void rejectsUnknownShootMode(){
        var source=shootingEntity("a","2","3",5);
        when(weapons.findByIdAndCharacterIdForUpdate("a","c1")).thenReturn(Optional.of(source));

        assertThatThrownBy(()->service.shoot("c1","a",new WeaponInventoryService.ShootCommand("burst",null)))
                .hasMessageContaining("Modo");
    }
    @Test void treatsWeaponOwnedByAnotherCharacterAsNotFound(){
        when(weapons.findByIdAndCharacterIdForUpdate("a","c1")).thenReturn(Optional.empty());

        assertThatThrownBy(()->service.shoot("c1","a",new WeaponInventoryService.ShootCommand("normal",1)))
                .isInstanceOf(NoSuchElementException.class).hasMessage("Arma no encontrada");
    }
}

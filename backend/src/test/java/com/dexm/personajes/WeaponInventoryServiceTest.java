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
    WeaponRequest request(String slot,String size){return new WeaponRequest(slot,"Arma","PISTOLA",size,bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),"9mm",null);}
    BigDecimal bd(int n){return BigDecimal.valueOf(n);}
    WeaponEntity entity(String id,String slot,String size){return new WeaponEntity(id,"c1",slot,"Arma","PISTOLA",size,bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),"9mm",null);}
    @Test void createsAndReplacesSlot(){when(weapons.save(any())).thenAnswer(i->i.getArgument(0));when(weapons.findByCharacterIdAndSlot("c1","SMALL_1")).thenReturn(Optional.empty());var result=service.create("c1",request("SMALL_1","PEQUENA"));assertThat(result).containsEntry("slot","SMALL_1");verify(weapons).save(any());}
    @Test void rejectsIncompatibleSize(){assertThatThrownBy(()->service.create("c1",request("MEDIUM_1","GRANDE"))).hasMessageContaining("no cabe");}
    @Test void swapsCompatibleWeapons(){var source=entity("a","SMALL_1","PEQUENA");var target=entity("b","MEDIUM_1","PEQUENA");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));when(weapons.findByCharacterIdAndSlot("c1","MEDIUM_1")).thenReturn(Optional.of(target));when(weapons.save(any())).thenAnswer(i->i.getArgument(0));service.move("c1","a","MEDIUM_1");assertThat(source.getSlot()).isEqualTo("MEDIUM_1");assertThat(target.getSlot()).isEqualTo("SMALL_1");}
    @Test void rejectsSwapWhenTargetDoesNotFitSourceSlot(){var source=entity("a","ANY","GRANDE");var target=entity("b","SMALL_1","PEQUENA");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));when(weapons.findByCharacterIdAndSlot("c1","SMALL_1")).thenReturn(Optional.of(target));assertThatThrownBy(()->service.move("c1","a","SMALL_1")).hasMessageContaining("no cabe");}
    @Test void preventsEditingToSizeThatNoLongerFitsItsSlot(){var source=entity("a","SMALL_1","PEQUENA");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));assertThatThrownBy(()->service.update("c1","a",request("ANY","GRANDE"))).hasMessageContaining("no cabe");}
    @Test void preventsChangingTypeOfCatalogWeapon(){var source=new WeaponEntity("a","c1","SMALL_1","Beretta 92","PISTOLA","PEQUENA",bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),"9mm",null,"catalog-1","/weapons/beretta.jpg");when(weapons.findByIdAndCharacterId("a","c1")).thenReturn(Optional.of(source));var changed=new WeaponRequest("SMALL_1","Beretta 92","SUBFUSIL","PEQUENA",bd(10),bd(1),"2",bd(4),bd(3),bd(2),bd(1),null,null,bd(8),"9mm",null);assertThatThrownBy(()->service.update("c1","a",changed)).hasMessageContaining("tipo");}
}

package com.dexm.personajes.application;
import com.dexm.personajes.adapter.out.persistence.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service public class CampaignService {
 private final CampaignRepository campaigns; private final CharacterService characters;
 public CampaignService(CampaignRepository campaigns,CharacterService characters){this.campaigns=campaigns;this.characters=characters;}
 public List<CampaignEntity> list(){return campaigns.findAll();}
 @Transactional public CampaignEntity create(String name){return campaigns.save(new CampaignEntity(UUID.randomUUID().toString(),name.trim()));}
 public CampaignEntity get(String id){return campaigns.findById(id).orElseThrow(()->new NoSuchElementException("Campaign not found"));}
 @Transactional public void delete(String id){get(id);characters.deleteByCampaign(id);campaigns.deleteById(id);}
 public List<?> characters(String id){get(id);return characters.listByCampaign(id);}
 @Transactional public Object createCharacter(String id,String name,String imageUrl){get(id);return characters.create(id,name,imageUrl);}
}

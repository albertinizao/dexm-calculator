package com.dexm.personajes.domain;
import java.math.*; import java.util.*;
/** Pure trajectory rules. Intervals use character ages. */
public final class TrainingRules {
 private TrainingRules(){}
 public record Activity(String type,int startAge,int endAge,int priority,String primary,String secondary,String tertiary,boolean concurrent){}
 public record Bonus(BigDecimal primary,BigDecimal secondary,BigDecimal tertiary){}
 public static void validateProfile(Integer start,Integer awakening,Integer sheet,boolean einherjer,String origin){
  if(start==null||sheet==null||start<0||sheet<=start)throw new IllegalArgumentException("Las edades inicial y de ficha no son válidas");
  if(!einherjer){if(awakening!=null||origin!=null)throw new IllegalArgumentException("Un humano no puede tener despertar Einherjer");return;}
  if(!"converted".equals(origin)&&!"born_human".equals(origin)&&!"born_einherjer".equals(origin))throw new IllegalArgumentException("Origen Einherjer no válido");
  if(awakening!=null&&(awakening<start||awakening>sheet))throw new IllegalArgumentException("La edad de despertar no es válida");
 }
 /** Legacy helper for characters whose minimum course age is the standard 10. */
 public static int courseSlots(int sheetAge){return courseSlots(10,sheetAge);}
 /** Courses are granted every four years, including a partial final period. */
 public static int courseSlots(int startingAge,int sheetAge){int span=sheetAge-startingAge;return span<=0?0:(span+3)/4;}
 public static Bonus bonus(String type,double humanYears){String t=type.toUpperCase(Locale.ROOT);if("COURSE".equals(t))return b(2,0,0);int y=(int)Math.floor(humanYears+1e-9);if("FORMATION".equals(t))return y>=8?b(6,4,2):y>=6?b(5,3,2):y>=4?b(4,2,1):y>=2?b(3,1,0):y>=1?b(2,0,0):b(0,0,0);if("PROFESSION".equals(t))return y>=20?b(6,4,3):y>=15?b(5,3,2):y>=10?b(4,2,1):y>=5?b(3,1,0):y>=1?b(1,0,0):b(0,0,0);return y>=15?b(4,3,2):y>=10?b(3,2,2):y>=5?b(3,1,1):y>=3?b(2,1,0):y>=1?b(1,0,0):b(0,0,0);}
 private static Bonus b(int a,int b,int c){return new Bonus(BigDecimal.valueOf(a),BigDecimal.valueOf(b),BigDecimal.valueOf(c));}
 public static double humanEquivalent(Activity a,boolean einherjer,String origin,Integer awakening){if("COURSE".equalsIgnoreCase(a.type()))return 0;double total=0;for(int age=a.startAge();age<a.endAge();age++){double speed=!einherjer||awakening==null||age<awakening?1:("converted".equals(origin)?2:3);total+=speed;}return a.concurrent()&&"OCCUPATION".equalsIgnoreCase(a.type())?total/1.5:total;}
 public static BigDecimal coincidence(BigDecimal value,int previousSelections){return value.divide(BigDecimal.valueOf(2).pow(previousSelections),8,RoundingMode.HALF_UP);}
 public static int roundTotal(Collection<BigDecimal> values){return values.stream().reduce(BigDecimal.ZERO,BigDecimal::add).setScale(0,RoundingMode.HALF_UP).intValue();}
}

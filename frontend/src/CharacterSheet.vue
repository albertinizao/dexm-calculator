<script setup lang="ts">

import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { useRoute, useRouter } from 'vue-router';

import { api, type AllocationPayload, type Ammunition, type Armor, type ArmorCatalogItem, type ArmorSlot, type OtherInventoryItem, type PhysicalShield, type PhysicalShieldCatalogItem, type Shield, type ShieldCatalogItem, type Weapon, type WeaponCatalogItem } from './services/api';

const props = defineProps<{ isDirector: boolean }>();



type Character = {

  id: string; name: string; imageUrl?: string | null; campaignId?: string | null; einherjer?: boolean; awakened?: boolean; einherjerOrigin?: 'converted' | 'born_human' | 'born_einherjer' | null; startingAge?: number | null; awakeningAge?: number | null; sheetAge?: number | null;

  experience: number; level: number; attributes?: Record<string, number>; attributeTotals?: Record<string, number>; attributeModifiers?: Record<string, AttributeModifier[]>; derivedStats?: Record<string, DerivedStat>; genetics?: Record<string, number>; genetic?: Record<string, number>; allocation?: AllocationBudget;

  updatedAt?: string; createdAt?: string; closed?: boolean; lastClosedAt?: string; minorAttributes?: MinorAttribute[];
  abilities?: string[]; pendingUniqueAbilities?: string[];
  editorEmails?: string[]; canEdit?: boolean;

};

type Campaign = { id: string; name: string };
type CampaignMember = { id: string; email: string; active: boolean; createdAt: string; revokedAt?: string };
type MinorAttribute = { id:string; key:string; name:string; value:number; ranks:number; total:number; max:number; maxFormula?:string; bonusSource?:string; plusOne:number; plusD6:number; type:string };
type AllocationBudget = { evolutionAvailable:number; evolutionSpent:number; evolutionRemaining:number; geneticsAvailable:number; geneticsSpent:number; geneticsRemaining:number; nextEvolutionReward?:number; nextGeneticsReward?:number; minorEvolutionCost?:number };
type AllocationDraft = { level:number; experience:number; evolutionAvailable:number; geneticsAvailable:number; minorEvolutionCost:number; attributes:Record<string,number>; genetics:Record<string,number>; minorAttributes:Record<string,number>; baseAttributes:Record<string,number>; baseGenetics:Record<string,number>; baseMinorAttributes:Record<string,number> };
type AttributeModifier = { name: string; value: number; source?: string };
type DerivedStat = { key: string; name: string; formula: string; baseValue: number; total: number; modifiers?: AttributeModifier[] };
type Progression = { kind: string; number: number; threshold: number; obtained: boolean };
type AttributeDetail = { key:string; definitionId?:string | null; name:string; type:string; total:number; ranks:number; maxRanks:number | null; formula:string; calculatedValue:number; plusOne:number; plusD6:number; modifiers:AttributeModifier[]; progressions:Progression[]; deletable:boolean };
type AttributeRow = { key:string; name:string; value:number; definitionId?:string; deletable?:boolean };
type AttributeRollDie = { id:string; type:'d10' | 'd6'; value:number; selected:boolean; disabled?:boolean };
type AttributeRollState = { key:string; name:string; score:number; plusOne:number; plusD6:number; dice:AttributeRollDie[]; abilityName?:string; difficulty?:number|null; testName?:string };
type WeaponDamage = Pick<Weapon, 'damageVital' | 'damageNormal' | 'damageLight' | 'damageVeryLight'>;
type WeaponAimRoll = { id:string; dice:AttributeRollDie[]; damageD10:AttributeRollDie };
type WeaponAimRollState = { weaponName:string; score:number; plusOne:number; plusD6:number; weaponAim:number; damage:WeaponDamage; rolls:WeaponAimRoll[] };
type Ability = { name:string; description?:string; launchType?:string; cost?:number | string | null; test?:string; alternativesJson?:string; uniqueFlag?:string };
type PendingUniqueAbility = Ability & { requirements: unknown };
type LastUpgrade = { available:boolean; current?:{level:number; closedAt:string}; previous?:{level:number; closedAt:string}; scores?:{key:string; type:string; before:number; after:number; increase:number}[]; bonuses?:{key:string; plusOne:number; plusD6:number}[]; modifiers?:{key:string; name:string; before:number|null; after:number|null}[]; abilities?:string[] };
type HistoryVersion = { id:string; level:number; experience:number; createdAt:string; snapshot: Record<string, any> };
type TrainingActivity = { id:string; type:string; name:string; startAge:number; endAge:number; priority:number; concurrent:boolean; primaryAttribute?:string|null; secondaryAttribute?:string|null; tertiaryAttribute?:string|null; modifiers?:{attributeKey:string;name:string;value:number}[] };
type TrainingPreview = { humanYears:number; modifiers:{attributeKey:string; baseValue:number; previousSelections:number; value:number}[] };



const route = useRoute();

const router = useRouter();

const character = ref<Character | null>(null);
const editing = ref(false);
const canEdit = computed(() => props.isDirector || character.value?.canEdit === true);
const editorEmails = ref<string[]>([]);
const campaignMembers = ref<CampaignMember[]>([]);
const editorBusy = ref(false);
const editorError = ref('');
const campaignEditorCandidates = computed(() => [...new Set(campaignMembers.value.filter(member => member.active).map(member => member.email))]);

const campaign = ref<Campaign | null>(null);

const loading = ref(true);

const error = ref('');

const attributeDetail = ref<AttributeDetail | null>(null);
const detailLoading = ref(false);
const detailError = ref('');
const showAttributeDetail = ref(false);
const showAttributeRoll = ref(false);
const attributeRoll = ref<AttributeRollState | null>(null);
const deletingAttribute = ref(false);
const modifierDraft = ref<Record<string, AttributeModifier[]>>({});
const modifierSaveBusy = ref(false);
const modifierError = ref('');
const showExperienceModal = ref(false);
const experienceAmount = ref<number | null>(null);
const experienceBusy = ref(false);
const experienceError = ref('');
const levelBusy = ref(false);
const levelError = ref('');
const closeBusy = ref(false);
const closeError = ref('');
const showAllocationModal = ref(false);
const allocationMode = ref<'single' | 'all'>('single');
const allocationStep = ref(1);
const allocationTotal = ref(1);
const allocationDraft = ref<AllocationDraft | null>(null);
const allocationModal = ref<HTMLElement | null>(null);
const abilityCatalog = ref<Ability[]>([]);
const abilityCatalogLoading = ref(false);
const abilityCatalogError = ref('');
const sheetView = ref<'sheet' | 'abilities' | 'inventory' | 'inventory-type' | 'inventory-detail' | 'ammunition-detail' | 'weapon-choice' | 'weapon-catalog' | 'weapon-detail' | 'armor-choice' | 'armor-catalog' | 'armor-detail' | 'shield-choice' | 'shield-catalog' | 'shield-detail' | 'physical-shield-detail'>('sheet');
const otherInventory = ref<OtherInventoryItem[]>([]);
const inventoryLoading = ref(false);
const inventoryError = ref('');
const selectedOtherItem = ref<OtherInventoryItem | null>(null);
const inventoryDraft = ref<Omit<OtherInventoryItem, 'id'>>({ name: '', description: '', location: '', quantity: 1, unitValue: 0 });
const inventorySaving = ref(false);
const inventoryDeleting = ref(false);
const ammunition = ref<Ammunition[]>([]);
const ammunitionCalibers = ref<string[]>([]);
const selectedAmmunition = ref<Ammunition | null>(null);
const ammunitionDraft = ref<Omit<Ammunition, 'id'>>({ caliber: '', quantity: 1 });
const ammunitionSaving = ref(false);
const ammunitionDeleting = ref(false);
const weapons = ref<Weapon[]>([]);
const weaponDraft = ref<Omit<Weapon, 'id'>>({slot:'SMALL_1',name:'',weaponType:'PISTOLA',size:'PEQUENA',range:0,reload:0,rate:'',damageVital:0,damageNormal:0,damageLight:0,damageVeryLight:0,aim:null,automaticFire:'',capacity:0,loadedBullets:0,caliber:'',extraRule:''});
const catalogWeapons = ref<WeaponCatalogItem[]>([]); const catalogSearch = ref(''); const catalogType = ref(''); const catalogLoading = ref(false); const catalogSlot = ref('SMALL_1'); const customImageUrl = ref<string | null>(null);
const selectedWeapon = ref<Weapon | null>(null);
const showWeaponDetailModal = ref(false);
const weaponEditMode = ref(false);
const selectedCatalogWeapon = ref<WeaponCatalogItem | null>(null);
const showCatalogWeaponModal = ref(false);
const weaponSlotLocked = ref(false);
const weaponSaving = ref(false); const weaponDeleting = ref(false); const weaponMoving = ref(false);
const weaponReloading = ref(false); const weaponShooting = ref(false);
const shootWeaponTarget = ref<Weapon | null>(null); const showShootModal = ref(false);
const weaponAimRoll = ref<WeaponAimRollState | null>(null); const showWeaponAimRollModal = ref(false);
let weaponAimRollSequence = 0;
const armors = ref<Armor[]>([]); const shields = ref<Shield[]>([]); const physicalShields = ref<PhysicalShield[]>([]);
const armorDraft = ref<Omit<Armor, 'id'>>({ name:'', description:'', slots:[], rdBySlot:{HEAD:0,BODY:0,LEGS:0,ARMS:0}, armorBySlot:{HEAD:0,BODY:0,LEGS:0,ARMS:0}, imageUrl:null });
const shieldDraft = ref<Omit<Shield, 'id'>>({ name:'', description:'', hitPoints:0, imageUrl:null });
const physicalShieldDraft = ref<Omit<PhysicalShield, 'id'>>({ name:'', description:'', rd:0, armor:0, defense:0, otherEffects:'', imageUrl:null });
const catalogArmors = ref<ArmorCatalogItem[]>([]); const catalogShields = ref<ShieldCatalogItem[]>([]);
const selectedArmor = ref<Armor | null>(null); const selectedShield = ref<Shield | null>(null);
const selectedPhysicalShield = ref<PhysicalShield | null>(null);
const showArmorDetailModal = ref(false);
const showShieldDetailModal = ref(false);
const showPhysicalShieldDetailModal = ref(false);
const selectedCatalogArmor = ref<ArmorCatalogItem | null>(null); const selectedCatalogShield = ref<ShieldCatalogItem | null>(null);
const showCatalogArmorModal = ref(false); const showCatalogShieldModal = ref(false);
const armorCatalogMode = ref(false); const shieldCatalogMode = ref(false);
const armorImageUrl = ref<string | null>(null); const shieldImageUrl = ref<string | null>(null);
const armorCatalogLoading = ref(false); const shieldCatalogLoading = ref(false);
const armorSaving = ref(false); const armorDeleting = ref(false); const shieldSaving = ref(false); const shieldDeleting = ref(false);
const armorSlots = [{value:'HEAD' as ArmorSlot,label:'Cabeza'},{value:'BODY' as ArmorSlot,label:'Cuerpo'},{value:'LEGS' as ArmorSlot,label:'Piernas'},{value:'ARMS' as ArmorSlot,label:'Brazos'}];
const occupiedArmorSlots = computed(() => new Set(armors.value.filter(item => !selectedArmor.value || item.id !== selectedArmor.value.id).flatMap(item => item.slots)));
const weaponTypes = [{value:'PISTOLA',label:'Pistola'},{value:'SUBFUSIL',label:'Subfusil'},{value:'FUSIL',label:'Fusil'},{value:'RIFLE_CAZA',label:'Rifle de caza'},{value:'FUSIL_FRANCOTIRADOR',label:'Fusil de francotirador'},{value:'AMETRALLADORA_LIGERA',label:'Ametralladora ligera'},{value:'ESCOPETA',label:'Escopeta'},{value:'CUERPO_PEQUENA',label:'Cuerpo a cuerpo pequeña'},{value:'CUERPO_MEDIANA',label:'Cuerpo a cuerpo mediana'},{value:'CUERPO_PESADA',label:'Cuerpo a cuerpo pesada'}];
const weaponSizes = [{value:'PEQUENA',label:'Pequeña'},{value:'MEDIANA',label:'Mediana'},{value:'GRANDE',label:'Grande'},{value:'ENORME',label:'Enorme'}];
const weaponSlots = [{value:'SMALL_1',label:'Pequeña 1',kind:'small'},{value:'SMALL_2',label:'Pequeña 2',kind:'small'},{value:'SMALL_3',label:'Pequeña 3',kind:'small'},{value:'MEDIUM_1',label:'Mediana 1',kind:'medium'},{value:'MEDIUM_2',label:'Mediana 2',kind:'medium'},{value:'ANY',label:'Universal',kind:'any'}];
const selectedAbility = ref<Ability | null>(null);
const showLastUpgrade = ref(false); const lastUpgrade = ref<LastUpgrade | null>(null); const lastUpgradeLoading = ref(false); const lastUpgradeError = ref('');
const currentUpgradeMode = ref(false);
const showLegacyImport = ref(false);
const legacyCode = ref('');
const legacyBusy = ref(false);
const legacyError = ref('');
const legacyExportCode = ref('');
const showLegacyExport = ref(false);
const legacyDraft = ref(false);
const legacyEvolutionPoints = ref<number | null>(null);
const showUniqueReview = ref(false);
const pendingUniqueAbilities = ref<PendingUniqueAbility[]>([]);
const uniqueReviewBusy = ref('');
const uniqueReviewError = ref('');
const showHistory = ref(false);
const history = ref<HistoryVersion[]>([]);
const selectedHistory = ref<HistoryVersion | null>(null);
const historyLoading = ref(false);
const historyError = ref('');
const historyRecovering = ref('');
const cancelChangesBusy = ref(false);
const showTrainingModal = ref(false); const trainingLoading = ref(false); const trainingError = ref('');
const showTrainingForm = ref(false);
const trainingData = ref<{enabled:boolean;startingAge:number;sheetAge:number;activities:TrainingActivity[]}>({enabled:false,startingAge:0,sheetAge:0,activities:[]});
const trainingDraft = ref<Partial<TrainingActivity>>({ type:'FORMATION', name:'', startAge:0, endAge:1, priority:0, concurrent:false, primaryAttribute:'', secondaryAttribute:'', tertiaryAttribute:'' });
const trainingEditingId = ref<string|null>(null);
const trainingReordering = ref<string | null>(null);
const trainingPreview = ref<TrainingPreview | null>(null);
let trainingPreviewTimer: ReturnType<typeof setTimeout> | null = null;



const attributeLabels: Record<string, string> = {

  fisico: 'Físico', agilidad: 'Agilidad', percepcion: 'Percepción', mente: 'Mente', estudio: 'Estudio', carisma: 'Carisma',

  astronavegar: 'Astronavegar', atractivo: 'Atractivo', buscar: 'Buscar', conduccion: 'Conducción', cruzarbifrost: 'Cruzar Bifrost',

  deporte: 'Deporte', destreza: 'Destreza', diplomacia: 'Diplomacia', einherjer: 'Einherjer', engano: 'Engaño', esconderse: 'Esconderse',

  evolcurva: 'Evolución curva', esquiva: 'Esquiva', fisicaquimica: 'Física/Química', fuerza: 'Fuerza', informatica: 'Informática',

  intimidar: 'Intimidar', labia: 'Labia', liderazgo: 'Liderazgo', medicina: 'Medicina', provocar: 'Provocar', punteria: 'Puntería',

  resistencia: 'Resistencia', sentiryggdrasil: 'Sentir Yggdrasil',
  vida: 'Vida máxima', bifrost: 'Bifrost máximo', defensaCuerpo: 'Defensa cuerpo a cuerpo', defensaDistancia: 'Defensa a distancia',

};

const geneticLabels: Record<string, string> = { heroe: 'Héroe', norna: 'Norna', alfar: 'Alfar', valkiria: 'Valkiria', risa: 'Risi', dvergr: 'Dvergr' };

const geneticGroups = [

  { label: 'Aesir', keys: ['heroe', 'norna'] },

  { label: 'Vanir', keys: ['alfar', 'valkiria'] },

  { label: 'Jotun', keys: ['risa', 'dvergr'] },

];

const majorKeys = ['fisico', 'agilidad', 'percepcion', 'mente', 'estudio', 'carisma'];

const minorKeys = ['astronavegar', 'atractivo', 'buscar', 'conduccion', 'cruzarbifrost', 'deporte', 'destreza', 'diplomacia', 'einherjer', 'engano', 'esconderse', 'evolcurva', 'esquiva', 'fisicaquimica', 'fuerza', 'informatica', 'intimidar', 'labia', 'liderazgo', 'medicina', 'provocar', 'punteria', 'resistencia', 'sentiryggdrasil'];

const minorCapFormulas: Record<string, string> = {
  astronavegar: '', atractivo: 'carisma*2', buscar: 'percepcion', conduccion: '(agilidad+percepcion)/2', cruzarbifrost: '(mente+estudio)/2',
  deporte: 'fisico', destreza: '(fisico+agilidad)/2', diplomacia: 'carisma', einherjer: '(fisico+mente)/2', engano: '(percepcion+mente)/2',
  esconderse: '(agilidad+mente)/2', evolcurva: '(fisico+agilidad+percepcion+mente+estudio+carisma)/6', esquiva: '(fisico+agilidad)/2', fisicaquimica: 'estudio', fuerza: 'fisico', informatica: 'estudio',
  intimidar: '(fisico+carisma)/2', labia: 'carisma', liderazgo: 'carisma', medicina: 'estudio', provocar: '(mente+max(fisico,carisma))/2',
  punteria: 'percepcion', resistencia: 'fisico', sentiryggdrasil: '(percepcion+mente)/2',
};

const majorOneThresholds = [5, 11, 17, 23, 29, 35, 41, 47, 53, 59, 65, 71];

const majorD6Thresholds = [2, 8, 14, 20, 26, 32, 38, 44, 50, 56, 62, 68];

const minorOneThresholds: Record<string, number[]> = {

  atractivo: [3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55],

  buscar: [5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60],

  conduccion: [3, 12, 19, 29, 37, 46, 53, 61, 73, 80, 93, 110],

  cruzarbifrost: [6, 13, 21, 30, 40, 51, 63, 76, 90, 105, 121, 138],

  deporte: [6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72],

  destreza: [3, 8, 13, 18, 23, 28, 33, 38, 43, 48, 53, 58],

  diplomacia: [3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55],

  einherjer: [6, 13, 21, 30, 40, 51, 63, 76, 90, 105, 121, 138],

  engano: [3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55],

  esconderse: [9, 16, 23, 30, 37, 44, 51, 58, 65, 72, 79, 86],

  evolcurva: [5, 15, 25, 35, 45, 55, 65, 75, 85, 95, 105, 115],

  esquiva: [2, 5, 8, 11, 15, 19, 23, 28, 33, 38, 44, 50],

  fisicaquimica: [4, 10, 17, 24, 31, 38, 45, 52, 59, 66, 73, 80],

  fuerza: [6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72],

  informatica: [4, 10, 17, 24, 31, 38, 45, 52, 59, 66, 73, 80],

  intimidar: [7, 13, 19, 25, 31, 37, 43, 49, 55, 61, 67, 73],

  labia: [3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55],

  liderazgo: [3, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55],

  medicina: [4, 10, 17, 24, 31, 38, 45, 52, 59, 66, 73, 80],

  provocar: [7, 13, 19, 25, 31, 37, 43, 49, 55, 61, 67, 73],

  punteria: [9, 16, 23, 30, 37, 44, 51, 58, 65, 72, 79, 86],

  resistencia: [9, 16, 23, 30, 37, 44, 51, 58, 65, 72, 79, 86],

  sentiryggdrasil: [6, 13, 21, 30, 40, 51, 63, 76, 90, 105, 121, 138],

};

const minorD6Thresholds: Record<string, number[]> = {

  atractivo: [2, 7, 12, 17, 22, 27, 32, 37, 42, 47, 52, 57],

  buscar: [2, 7, 12, 17, 22, 27, 32, 37, 42, 47, 52, 57],

  conduccion: [1, 5, 5, 10, 17, 25, 33, 40, 50, 57, 65, 78],

  cruzarbifrost: [3, 8, 14, 21, 29, 38, 48, 59, 71, 83, 97, 110],

  deporte: [2, 9, 15, 21, 27, 33, 39, 45, 51, 57, 63, 69],

  destreza: [3, 10, 17, 24, 31, 38, 45, 52, 59, 56, 73, 80],

  diplomacia: [2, 8, 14, 20, 26, 32, 38, 44, 50, 56, 62, 68],

  einherjer: [3, 8, 14, 21, 29, 38, 48, 59, 71, 83, 97, 110],

  engano: [2, 8, 14, 20, 26, 32, 38, 44, 50, 56, 62, 68],

  esconderse: [3, 10, 19, 26, 33, 40, 47, 54, 61, 68, 75, 82],

  evolcurva: [10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120],

  esquiva: [7, 15, 23, 31, 39, 47, 55, 63, 71, 79, 87, 95],

  fisicaquimica: [2, 6, 13, 20, 27, 34, 41, 48, 55, 62, 69, 76],

  fuerza: [2, 9, 15, 21, 27, 33, 39, 45, 51, 57, 63, 69],

  informatica: [2, 6, 13, 20, 27, 34, 41, 48, 55, 62, 69, 76],

  intimidar: [2, 8, 14, 20, 26, 32, 38, 44, 50, 56, 62, 68],

  labia: [2, 8, 14, 20, 26, 32, 38, 44, 50, 56, 62, 68],

  liderazgo: [2, 8, 14, 20, 26, 32, 38, 44, 50, 56, 62, 68],

  medicina: [2, 6, 13, 20, 27, 34, 41, 48, 55, 62, 69, 76],

  provocar: [2, 8, 14, 20, 26, 32, 38, 44, 50, 56, 62, 68],

  punteria: [2, 9, 15, 21, 27, 33, 39, 45, 51, 57, 63, 69],

  resistencia: [2, 9, 15, 21, 27, 33, 39, 45, 51, 57, 63, 69],

  sentiryggdrasil: [3, 8, 14, 21, 29, 38, 48, 59, 71, 83, 97, 110],

};

const initials = (name: string) => name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();

const level = computed(() => character.value?.level ?? 1);
const canLevelUp = computed(() => (character.value?.experience ?? 0) >= 100);
const canLevelUpAll = computed(() => (character.value?.experience ?? 0) >= 200);

const attributes = computed(() => character.value?.attributes ?? {});

const genetics = computed(() => character.value?.genetics ?? character.value?.genetic ?? {});
const geneticTotals = computed(() => Object.fromEntries(Object.keys(genetics.value).map(key => [key, character.value?.attributeTotals?.[key] ?? genetics.value[key] ?? 0])));

// The sheet must render the effective score (base ranks plus every persisted
// modifier), not only the base attribute map.  `displayedAttributeTotal`
// still applies the in-progress manual draft while editing.
const majorAttributes = computed(() => {
  const totals = character.value?.attributeTotals ?? {};
  return majorKeys.map(key => [key, totals[key] ?? attributes.value[key] ?? 0] as [string, number]);
});

const minorAttributes = computed(() => { const totals=character.value?.attributeTotals ?? {}; const built=minorKeys.filter(k=>k!=='astronavegar').map(key => [key, totals[key] ?? attributes.value[key] ?? 0] as [string, number]); const custom=(character.value?.minorAttributes??[]).map(a=>[a.key,a.total ?? a.value] as [string,number]); return [...built,...custom]; });
const derivedStatKeys = ['vida', 'bifrost', 'defensaCuerpo', 'defensaDistancia'];
const trainingAttributeOptions = computed(() => minorAttributes.value.map(([key]) => ({ key, label: attributeLabels[key] || customMinor(key)?.name || key })));
const trainingAgeOptions = computed(() => Array.from({ length: Math.max(0, trainingData.value.sheetAge - trainingData.value.startingAge), }, (_, index) => trainingData.value.startingAge + index));
const trainingBlockingActivities = computed(() => trainingData.value.activities.filter(activity => activity.id !== trainingEditingId.value && activity.type !== 'COURSE' && !(activity.type === 'OCCUPATION' && activity.concurrent)));
function trainingIntervalAllowed(startAge:number, endAge:number) { return trainingDraft.value.type === 'COURSE' || (trainingDraft.value.type === 'OCCUPATION' && Boolean(trainingDraft.value.concurrent)) || !trainingBlockingActivities.value.some(activity => startAge < activity.endAge && activity.startAge < endAge); }
const trainingEndAgeOptions = computed(() => {
  const startAge = Number(trainingDraft.value.startAge ?? trainingData.value.startingAge);
  const maximumFormationDuration = character.value?.einherjer ? 4 : 8;
  const maximumEndAge = trainingDraft.value.type === 'FORMATION'
    ? Math.min(trainingData.value.sheetAge, startAge + maximumFormationDuration)
    : trainingData.value.sheetAge;

  return Array.from({ length: Math.max(0, maximumEndAge - trainingData.value.startingAge + 1) }, (_, index) => trainingData.value.startingAge + index)
    .filter(age => age > startAge && trainingIntervalAllowed(startAge, age));
});
const trainingStartAgeOptions = computed(() => trainingAgeOptions.value.filter(startAge => Array.from({ length: Math.max(0, trainingData.value.sheetAge - startAge + 1) }, (_, index) => startAge + index + 1).some(endAge => trainingIntervalAllowed(startAge, endAge))));
const trainingDisplayActivities = computed(() => [...trainingData.value.activities].sort((a, b) => Number(a.type === 'COURSE') - Number(b.type === 'COURSE') || a.startAge - b.startAge || a.priority - b.priority));
const trainingCoreActivities = computed(() => trainingDisplayActivities.value.filter(activity => activity.type !== 'COURSE'));
const trainingCourseActivities = computed(() => trainingDisplayActivities.value.filter(activity => activity.type === 'COURSE'));
const trainingCourseSlots = computed(() => { const span = trainingData.value.sheetAge - trainingData.value.startingAge; return span <= 0 ? 0 : Math.ceil(span / 4); });
const trainingCourseUsed = computed(() => trainingData.value.activities.filter(activity => activity.type === 'COURSE').length);
const trainingTotalModifiers = computed(() => trainingDisplayActivities.value.flatMap(activity => (activity.modifiers ?? []).map(modifier => ({ ...modifier, activityName: activity.name, activityType: activity.type }))));
const trainingTotalByAttribute = computed(() => { const grouped = new Map<string, { key:string; total:number; modifiers:{ activityName:string; value:number }[] }>(); for (const modifier of trainingTotalModifiers.value) { const group = grouped.get(modifier.attributeKey) ?? { key:modifier.attributeKey, total:0, modifiers:[] }; group.total += Number(modifier.value) || 0; group.modifiers.push({ activityName:modifier.activityName, value:Number(modifier.value) || 0 }); grouped.set(modifier.attributeKey, group); } return [...grouped.values()]; });
const derivedStats = computed(() => character.value?.derivedStats ?? {});

const allocationMajorAttributes = computed(() => majorKeys.map(key => [key, allocationDraft.value?.attributes[key] ?? 0] as [string, number]));
const allocationMinorAttributes = computed(() => {
  if (!allocationDraft.value) return [] as [string, number][];
  const custom = (character.value?.minorAttributes ?? []).map(attribute => [attribute.key, allocationDraft.value?.minorAttributes[attribute.key] ?? 0] as [string, number]);
  return [...minorKeys.map(key => [key, allocationDraft.value?.attributes[key] ?? 0] as [string, number]), ...custom];
});
const allocationGenetics = computed(() => geneticGroups.flatMap(group => group.keys).map(key => [key, allocationDraft.value?.genetics[key] ?? 0] as [string, number]));
const allocationBudget = computed<AllocationBudget>(() => {
  const draft = allocationDraft.value;
  if (!draft) return { evolutionAvailable: 0, evolutionSpent: 0, evolutionRemaining: 0, geneticsAvailable: 0, geneticsSpent: 0, geneticsRemaining: 0 };
  const evolutionSpent = majorKeys.reduce((sum, key) => sum + Math.max(0, (Number(draft.attributes[key]) || 0) - (Number(draft.baseAttributes[key]) || 0)) * 10, 0)
    + minorKeys.reduce((sum, key) => sum + Math.max(0, (Number(draft.attributes[key]) || 0) - (Number(draft.baseAttributes[key]) || 0)) * 5, 0)
  + Object.keys(draft.minorAttributes).reduce((sum, key) => sum + Math.max(0, (Number(draft.minorAttributes[key]) || 0) - (Number(draft.baseMinorAttributes[key]) || 0)) * 5, 0)
  - (draft.minorEvolutionCost === 4 ? Math.max(0, (Number(draft.attributes.fuerza) || 0) - (Number(draft.baseAttributes.fuerza) || 0)) : 0);
  const geneticsSpent = Object.keys(draft.genetics).reduce((sum, key) => sum + Math.max(0, (Number(draft.genetics[key]) || 0) - (Number(draft.baseGenetics[key]) || 0)), 0);
  const geneticsAvailable = draft.geneticsAvailable;
  return { evolutionAvailable: draft.evolutionAvailable, evolutionSpent, evolutionRemaining: draft.evolutionAvailable - evolutionSpent, geneticsAvailable, geneticsSpent, geneticsRemaining: geneticsAvailable - geneticsSpent };
});
const allocationValid = computed(() => allocationBudget.value.evolutionRemaining >= 0 && allocationBudget.value.geneticsRemaining === 0);

function evaluateAllocationFormula(formula: string, draft: AllocationDraft): number {
  if (!formula) return Number.POSITIVE_INFINITY;
  const tokens = formula.match(/[A-Za-z_][A-Za-z0-9_]*|\d+|[()+\-*/ ,]/g)?.map(token => token.trim()).filter(Boolean) ?? [];
  let position = 0;
  const valueOf = (name: string) => draft.attributes[name] ?? draft.genetics[name] ?? draft.minorAttributes[name] ?? (name === 'nivel' ? draft.level : 0);
  const parseExpression = (): number => { let value = parseTerm(); while (tokens[position] === '+' || tokens[position] === '-') { const op = tokens[position++]; const right = parseTerm(); value = op === '+' ? value + right : value - right; } return value; };
  const parseTerm = (): number => { let value = parseFactor(); while (tokens[position] === '*' || tokens[position] === '/') { const op = tokens[position++]; const right = parseFactor(); value = op === '*' ? value * right : (right === 0 ? 0 : Math.floor(value / right)); } return value; };
  const parseFactor = (): number => {
    if (tokens[position] === '-') { position++; return -parseFactor(); }
    if (tokens[position] === '(') { position++; const value = parseExpression(); if (tokens[position] === ')') position++; return value; }
    const token = tokens[position++];
    if (!token) return 0;
    if (/^\d+$/.test(token)) return Number(token);
    if ((token === 'min' || token === 'max') && tokens[position] === '(') { position++; const values = [parseExpression()]; while (tokens[position] === ',') { position++; values.push(parseExpression()); } if (tokens[position] === ')') position++; return token === 'min' ? Math.min(...values) : Math.max(...values); }
    return valueOf(token);
  };
  return Math.max(0, Math.floor(parseExpression()));
}

function customAllocationMinor(key: string) { return character.value?.minorAttributes?.find(attribute => attribute.key === key); }
function allocationMinorCap(key: string): number | null {
  const draft = allocationDraft.value;
  if (!draft) return null;
  const formula = customAllocationMinor(key)?.maxFormula ?? minorCapFormulas[key];
  return formula ? evaluateAllocationFormula(formula, draft) : null;
}
function allocationRank(key: string): number {
  const draft = allocationDraft.value;
  if (!draft) return 0;
  return draft.attributes[key] ?? draft.minorAttributes[key] ?? 0;
}
function allocationModifierTotal(key: string): number {
  return (character.value?.attributeModifiers?.[key] ?? []).reduce((sum, modifier) => sum + (Number(modifier.value) || 0), 0);
}
function allocationScore(key: string): number {
  return allocationRank(key) + allocationModifierTotal(key);
}
function allocationSourceScore(source: string): number {
  const draft = allocationDraft.value;
  if (!draft) return 0;
  return (draft.attributes[source] ?? draft.genetics[source] ?? draft.minorAttributes[source] ?? 0) + allocationModifierTotal(source);
}
function allocationBonuses(key: string): { plusOne: number; plusD6: number } {
  const custom = customAllocationMinor(key);
  if (custom) {
    if (custom.type === 'GALDR') return { plusOne: Math.floor(allocationScore(key) / 5), plusD6: Math.floor(allocationScore(key) / 3) };
    const source = custom.bonusSource || key;
    const sourceScore = allocationSourceScore(source);
    return { plusOne: oneBonus(source, sourceScore, majorKeys.includes(source)), plusD6: d6Bonus(source, sourceScore, majorKeys.includes(source)) };
  }
  return { plusOne: oneBonus(key, allocationScore(key), majorKeys.includes(key)), plusD6: d6Bonus(key, allocationScore(key), majorKeys.includes(key)) };
}
function currentBonuses(key: string): { plusOne: number; plusD6: number } {
  const custom = customAllocationMinor(key);
  if (custom) return { plusOne: custom.plusOne, plusD6: custom.plusD6 };
  const rank = character.value?.attributes?.[key] ?? 0;
  const score = rank + allocationModifierTotal(key);
  return { plusOne: oneBonus(key, score, majorKeys.includes(key)), plusD6: d6Bonus(key, score, majorKeys.includes(key)) };
}
function allocationBonusChanged(key: string, kind: 'plusOne' | 'plusD6'): boolean {
  return allocationBonuses(key)[kind] !== currentBonuses(key)[kind];
}
function allocationCapFormula(key: string): string | null {
  if (key === 'evolcurva') return 'media atributos mayores';
  return customAllocationMinor(key)?.maxFormula ?? minorCapFormulas[key] ?? null;
}
function allocationAffordableMax(key: string, cost: number, baseline: Record<string, number>): number {
  const draft = allocationDraft.value;
  if (!draft) return baseline[key] ?? 0;
  const base = baseline[key] ?? 0;
  const current = draft.attributes[key] ?? draft.minorAttributes[key] ?? base;
  const pointsBeforeThisRank = allocationBudget.value.evolutionRemaining + Math.max(0, current - base) * cost;
  return base + Math.floor(Math.max(0, pointsBeforeThisRank) / cost);
}
function allocationEvolutionMax(key: string, custom: boolean): number {
  const baseline = custom ? allocationDraft.value?.baseMinorAttributes ?? {} : allocationDraft.value?.baseAttributes ?? {};
  const affordable = allocationAffordableMax(key, 5, baseline);
  const cap = allocationMinorCap(key);
  return cap === null ? affordable : Math.min(affordable, cap);
}
function allocationEvolutionAtLimit(key: string, custom: boolean): boolean {
  const draft = allocationDraft.value;
  if (!draft) return true;
  const value = custom ? draft.minorAttributes[key] ?? 0 : draft.attributes[key] ?? 0;
  const base = custom ? draft.baseMinorAttributes[key] ?? 0 : draft.baseAttributes[key] ?? 0;
  return value <= base && value >= allocationEvolutionMax(key, custom);
}
function allocationMajorAtLimit(key: string): boolean {
  const draft = allocationDraft.value;
  if (!draft) return true;
  const base = draft.baseAttributes[key] ?? 0;
  const value = draft.attributes[key] ?? 0;
  return value <= base && value >= allocationAffordableMax(key, 10, draft.baseAttributes);
}
function allocationGeneticMax(key: string): number {
  const draft = allocationDraft.value;
  if (!draft) return 0;
  const base = draft.baseGenetics[key] ?? 0;
  const value = draft.genetics[key] ?? base;
  const pointsBeforeThisRank = allocationBudget.value.geneticsRemaining + Math.max(0, value - base);
  return base + Math.min(2, Math.max(0, pointsBeforeThisRank));
}
function allocationGeneticAtLimit(key: string): boolean {
  const draft = allocationDraft.value;
  if (!draft) return true;
  const base = draft.baseGenetics[key] ?? 0;
  const value = draft.genetics[key] ?? base;
  return value <= base && value >= allocationGeneticMax(key);
}
function clampAllocationMinors() {
  const draft = allocationDraft.value;
  if (!draft) return;
  const customKeys = (character.value?.minorAttributes ?? []).map(attribute => attribute.key);
  for (let pass = 0; pass <= customKeys.length; pass++) {
    let changed = false;
    for (const key of minorKeys) {
      const cap = allocationMinorCap(key);
      if (cap !== null && draft.attributes[key] > cap) { draft.attributes[key] = cap; changed = true; }
    }
    for (const key of customKeys) {
      const cap = allocationMinorCap(key);
      if (cap !== null && draft.minorAttributes[key] > cap) { draft.minorAttributes[key] = cap; changed = true; }
    }
    if (!changed) break;
  }
}
function draftRank(value: unknown) { const parsed = Number(value); return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0; }
function updateAllocationAttribute(key: string, event: Event) { if (!allocationDraft.value) return; const major = majorKeys.includes(key); const base = allocationDraft.value.baseAttributes[key] ?? 0; const max = major ? allocationAffordableMax(key, 10, allocationDraft.value.baseAttributes) : allocationEvolutionMax(key, false); allocationDraft.value.attributes[key] = Math.min(max, Math.max(base, draftRank((event.target as HTMLInputElement).value))); clampAllocationMinors(); }
function updateAllocationMinor(key: string, event: Event) { if (!allocationDraft.value) return; allocationDraft.value.minorAttributes[key] = Math.min(allocationEvolutionMax(key, true), Math.max(allocationDraft.value.baseMinorAttributes[key] ?? 0, draftRank((event.target as HTMLInputElement).value))); clampAllocationMinors(); }
function updateAllocationGenetics(key: string, event: Event) { if (!allocationDraft.value) return; allocationDraft.value.genetics[key] = Math.min(allocationGeneticMax(key), Math.max(allocationDraft.value.baseGenetics[key] ?? 0, draftRank((event.target as HTMLInputElement).value))); }

function bonusCount(value: number, thresholds: number[]) {

  const score = Math.max(0, Number(value) || 0);

  return thresholds.filter(threshold => score >= threshold).length;

}

function oneBonus(key: string, value: number, major: boolean) {

  return bonusCount(value, major ? majorOneThresholds : (minorOneThresholds[key] ?? []));

}

function customMinor(key:string){ return character.value?.minorAttributes?.find(a=>a.key===key); }
function originLabel(origin?: string | null) { return ({ converted: 'Convertido', born_human: 'Nacido de humanos', born_einherjer: 'Nacido de Einherjer' } as Record<string, string>)[origin ?? ''] || 'No indicado'; }
function setAwakened(value: boolean) { if (!character.value) return; character.value.awakened = value; if (!value) character.value.awakeningAge = null; }
function openProfileModal() { showProfileModal.value = true; }
function openEditorsModal() { editorError.value = ''; showEditorsModal.value = true; }

const bonusSourceOptions = computed(() => {
  const keys = [...minorKeys.filter(key => key !== 'astronavegar'), ...(character.value?.minorAttributes ?? []).map(attribute => attribute.key)];
  return [...new Set(keys)].map(key => ({ key, label: attributeLabels[key] || customMinor(key)?.name || key }));
});

const formulaTokens = computed(() => [
  ...majorKeys,
  ...minorKeys.filter(key => key !== 'astronavegar'),
  ...(character.value?.minorAttributes ?? []).map(attribute => attribute.key),
  ...Array.from({ length: 10 }, (_, digit) => String(digit)),
  'nivel', '+', '-', '*', '/', '(', ')', ',', 'min(', 'max('
]);

const formulaIsValid = computed(() => {
  const formula = customFormula.value.trim();
  if (!formula || !/^[A-Za-z0-9_+*/().,\s-]+$/.test(formula)) return false;
  if (/\d{3,}/.test(formula)) return false;
  let depth = 0;
  for (const char of formula) {
    if (char === '(') depth++;
    if (char === ')' && --depth < 0) return false;
  }
  return depth === 0;
});

function maxBonus(key:string,value:number,major:boolean){ const c=customMinor(key); return c?.plusOne ?? oneBonus(key,value,major); }

function maxD6(key:string,value:number,major:boolean){ const c=customMinor(key); return c?.plusD6 ?? d6Bonus(key,value,major); }

function d6Bonus(key: string, value: number, major: boolean) {

  return bonusCount(value, major ? majorD6Thresholds : (minorD6Thresholds[key] ?? []));

}

function attributeRollDetails(key: string, fallback: number, major: boolean): Omit<AttributeRollState, 'dice'> {
  const custom = customMinor(key);
  const score = displayedAttributeTotal(key, custom?.total ?? fallback);
  return {
    key,
    name: attributeLabels[key] || custom?.name || key,
    score,
    plusOne: custom?.plusOne ?? oneBonus(key, score, major),
    plusD6: custom?.plusD6 ?? d6Bonus(key, score, major),
  };
}

function randomDieValue(sides: number): number {
  return Math.floor(Math.random() * sides) + 1;
}

function createAttributeRollDice(plusD6: number, idPrefix = ''): AttributeRollDie[] {
  const id = (suffix: string) => idPrefix ? `${idPrefix}-${suffix}` : suffix;
  const d10: AttributeRollDie = { id: id('d10'), type: 'd10', value: randomDieValue(10), selected: true };
  const d6Dice = Array.from({ length: 2 + Math.max(0, Math.floor(Number(plusD6) || 0)) }, (_, index) => ({
    id: id(`d6-${index + 1}`),
    type: 'd6' as const,
    value: randomDieValue(6),
    selected: false,
    disabled: false,
  }));
  const numberOfOnes = d6Dice.filter(die => die.value === 1).length;
  const diceToDisable = d6Dice
    .slice()
    .sort((left, right) => right.value - left.value || left.id.localeCompare(right.id))
    .slice(0, numberOfOnes);
  diceToDisable.forEach(die => { die.disabled = true; });
  const availableD6 = d6Dice.filter(die => !die.disabled);
  const selectedD6Ids = new Set(availableD6
    .slice()
    .sort((left, right) => right.value - left.value || left.id.localeCompare(right.id))
    .slice(0, 2)
    .map(die => die.id));
  const orderedD6Dice = d6Dice
    .slice()
    .sort((left, right) => right.value - left.value
      || Number(Boolean(left.disabled)) - Number(Boolean(right.disabled))
      || left.id.localeCompare(right.id));
  return [d10, ...orderedD6Dice.map(die => ({ ...die, selected: selectedD6Ids.has(die.id) }))];
}

function createWeaponAimRollDice(plusD6: number, rollId: string): AttributeRollDie[] {
  return createAttributeRollDice(plusD6, `aim-${rollId}`);
}

function createWeaponDamageDie(rollId: string): AttributeRollDie {
  return { id: `damage-${rollId}`, type: 'd10', value: randomDieValue(10), selected: true };
}

function openAttributeRoll(key: string, fallback: number, major: boolean) {
  const details = attributeRollDetails(key, fallback, major);
  attributeRoll.value = { ...details, dice: createAttributeRollDice(details.plusD6) };
  showAttributeRoll.value = true;
  nextTick(() => document.getElementById('attribute-roll-close')?.focus());
}

function openAbilityRoll(ability: Ability) {
  const test = abilityActivationDetails(ability);
  if (!test || test.score === null || test.difficulty === null) return;
  attributeRoll.value = {
    key: test.key,
    name: test.testName,
    score: test.score,
    plusOne: test.plusOne ?? 0,
    plusD6: test.plusD6 ?? 0,
    difficulty: test.difficulty,
    testName: test.testName,
    abilityName: ability.name,
    dice: createAttributeRollDice(test.plusD6 ?? 0),
  };
  showAttributeRoll.value = true;
  nextTick(() => document.getElementById('attribute-roll-close')?.focus());
}

function closeAttributeRoll() {
  showAttributeRoll.value = false;
  attributeRoll.value = null;
}

function rerollAttribute() {
  if (!attributeRoll.value) return;
  attributeRoll.value = { ...attributeRoll.value, dice: createAttributeRollDice(attributeRoll.value.plusD6) };
}

function toggleAttributeRollDie(id: string) {
  if (!attributeRoll.value) return;
  attributeRoll.value = {
    ...attributeRoll.value,
    dice: attributeRoll.value.dice.map(die => die.id === id && !die.disabled ? { ...die, selected: !die.selected } : die),
  };
}

function attributeRollDieImage(die: AttributeRollDie): string {
  const sides = die.type === 'd10' ? 'D10' : 'D6';
  return die.selected ? `/dice${sides}.png` : `/dice${sides}Blanco.png`;
}

const selectedAttributeRollD10 = computed(() => attributeRoll.value?.dice.find(die => die.type === 'd10' && die.selected)?.value ?? null);
const selectedAttributeRollD6 = computed(() => attributeRoll.value?.dice.filter(die => die.type === 'd6' && die.selected).map(die => die.value) ?? []);
const attributeRollD10Dice = computed(() => attributeRoll.value?.dice.filter(die => die.type === 'd10') ?? []);
const attributeRollD6Dice = computed(() => attributeRoll.value?.dice.filter(die => die.type === 'd6') ?? []);
const attributeRollAvailableD6 = computed(() => attributeRollD6Dice.value.filter(die => !die.disabled));
const attributeRollCanOmitD6 = computed(() => attributeRollAvailableD6.value.length < 2);
const attributeRollRequiredD6 = computed(() => Math.min(2, attributeRollAvailableD6.value.length));
const attributeRollUsesD6 = computed(() => selectedAttributeRollD6.value.length === attributeRollRequiredD6.value);
const attributeRollHasValidSelection = computed(() => selectedAttributeRollD10.value !== null && attributeRollUsesD6.value);
const attributeRollMissingSelection = computed(() => {
  const missing: string[] = [];
  if (selectedAttributeRollD10.value === null) missing.push('1 D10');
  if (selectedAttributeRollD6.value.length < attributeRollRequiredD6.value) missing.push(`${attributeRollRequiredD6.value - selectedAttributeRollD6.value.length} D6`);
  if (selectedAttributeRollD6.value.length > 2) missing.push(`deselecciona ${selectedAttributeRollD6.value.length - 2} D6`);
  return missing.join(' y ');
});
const attributeRollResult = computed(() => attributeRollHasValidSelection.value && attributeRoll.value
  ? selectedAttributeRollD10.value! + selectedAttributeRollD6.value.reduce((sum, value) => sum + value, 0) + attributeRoll.value.plusOne
  : null);
const attributeRollIsCritical = computed(() => attributeRollHasValidSelection.value
  && selectedAttributeRollD10.value === 10
  && selectedAttributeRollD6.value.length === 2
  && selectedAttributeRollD6.value.every(value => value === 6));
const attributeRollIsFumble = computed(() => attributeRollHasValidSelection.value && (attributeRollResult.value ?? 99) <= 3);
const abilityRollSuccess = computed(() => Boolean(
  attributeRoll.value?.abilityName
  && attributeRollHasValidSelection.value
  && attributeRoll.value.difficulty !== null
  && attributeRoll.value.difficulty !== undefined
  && attributeRollResult.value !== null
  && attributeRollResult.value >= attributeRoll.value.difficulty,
));

function openWeaponAimRolls(weapon: Weapon, rollCount: number) {
  const details = attributeRollDetails('punteria', character.value?.attributeTotals?.punteria ?? attributes.value.punteria ?? 0, false);
  const weaponAim = Number(weapon.aim) || 0;
  const rolls = Array.from({ length: Math.max(1, Math.floor(rollCount)) }, () => {
    const id = `${++weaponAimRollSequence}`;
    return { id, dice: createWeaponAimRollDice(details.plusD6, id), damageD10: createWeaponDamageDie(id) };
  });
  weaponAimRoll.value = {
    weaponName: weapon.name,
    score: details.score,
    plusOne: details.plusOne,
    plusD6: details.plusD6,
    weaponAim,
    damage: {
      damageVital: weapon.damageVital,
      damageNormal: weapon.damageNormal,
      damageLight: weapon.damageLight,
      damageVeryLight: weapon.damageVeryLight,
    },
    rolls,
  };
  showWeaponAimRollModal.value = true;
  nextTick(() => document.getElementById('weapon-aim-roll-close')?.focus());
}

function closeWeaponAimRolls() {
  showWeaponAimRollModal.value = false;
  weaponAimRoll.value = null;
}

function rerollWeaponAimRoll(rollId: string) {
  if (!weaponAimRoll.value) return;
  weaponAimRoll.value = {
    ...weaponAimRoll.value,
    rolls: weaponAimRoll.value.rolls.map(roll => roll.id === rollId
      ? {
        ...roll,
        dice: createWeaponAimRollDice(weaponAimRoll.value!.plusD6, roll.id),
        damageD10: createWeaponDamageDie(roll.id),
      }
      : roll),
  };
}

function toggleWeaponAimRollDie(rollId: string, dieId: string) {
  if (!weaponAimRoll.value) return;
  weaponAimRoll.value = {
    ...weaponAimRoll.value,
    rolls: weaponAimRoll.value.rolls.map(roll => roll.id === rollId
      ? { ...roll, dice: roll.dice.map(die => die.id === dieId && !die.disabled ? { ...die, selected: !die.selected } : die) }
      : roll),
  };
}

function weaponAimRollD10(roll: WeaponAimRoll): number | null {
  return roll.dice.find(die => die.type === 'd10' && die.selected)?.value ?? null;
}

function weaponAimRollD6(roll: WeaponAimRoll): number[] {
  return roll.dice.filter(die => die.type === 'd6' && die.selected).map(die => die.value);
}

function weaponAimRollIsValid(roll: WeaponAimRoll): boolean {
  const availableD6 = roll.dice.filter(die => die.type === 'd6' && !die.disabled).length;
  return weaponAimRollD10(roll) !== null && weaponAimRollD6(roll).length === Math.min(2, availableD6);
}

function weaponAimRollMissingSelection(roll: WeaponAimRoll): string {
  const missing: string[] = [];
  const selectedD6 = weaponAimRollD6(roll);
  if (weaponAimRollD10(roll) === null) missing.push('1 D10');
  const availableD6 = roll.dice.filter(die => die.type === 'd6' && !die.disabled).length;
  const requiredD6 = Math.min(2, availableD6);
  if (selectedD6.length < requiredD6) missing.push(`${requiredD6 - selectedD6.length} D6`);
  if (selectedD6.length > 2) missing.push(`deselecciona ${selectedD6.length - 2} D6`);
  return missing.join(' y ');
}

function weaponAimRollResult(roll: WeaponAimRoll): number | null {
  if (!weaponAimRoll.value || !weaponAimRollIsValid(roll)) return null;
  return weaponAimRollD10(roll)! + weaponAimRollD6(roll).reduce((sum, value) => sum + value, 0)
    + weaponAimRoll.value.plusOne + weaponAimRoll.value.weaponAim;
}

function weaponAimRollIsCritical(roll: WeaponAimRoll): boolean {
  return weaponAimRollIsValid(roll) && weaponAimRollD10(roll) === 10 && weaponAimRollD6(roll).length === 2 && weaponAimRollD6(roll).every(value => value === 6);
}

function weaponAimRollIsFumble(roll: WeaponAimRoll): boolean {
  const result = weaponAimRollResult(roll);
  return result !== null && result <= 3;
}

function weaponAimRollDamage(roll: WeaponAimRoll): { value:number; label:string } {
  const damageRoll = roll.damageD10.value;
  if (!weaponAimRoll.value) return { value: 0, label: '' };
  if (damageRoll === 10) return { value: weaponAimRoll.value.damage.damageVital, label: 'Vital' };
  if (damageRoll >= 6) return { value: weaponAimRoll.value.damage.damageNormal, label: 'Normal' };
  if (damageRoll >= 3) return { value: weaponAimRoll.value.damage.damageLight, label: 'Leve' };
  return { value: weaponAimRoll.value.damage.damageVeryLight, label: 'Muy leve' };
}

const savedAt = computed(() => character.value?.lastClosedAt ? new Date(character.value.lastClosedAt).toLocaleString('es-ES') : 'Sin guardar');

const characterRouteNames = {
  sheet: 'character-sheet',
  abilities: 'character-abilities',
  inventory: 'character-inventory',
} as const;

function queryWithEditMode(enabled: boolean) {
  const query = { ...route.query };
  if (enabled) query.mode = 'edit';
  else delete query.mode;
  return query;
}

function navigateToSection(section: keyof typeof characterRouteNames) {
  const name = characterRouteNames[section];
  if (route.name === name) return;
  router.push({ name, params: { id: String(route.params.id) }, query: { ...route.query } });
}

function closeHistory() {
  if (route.name === 'character-history') navigateToSection('sheet');
  else showHistory.value = false;
}



async function load() {

  loading.value = true; error.value = '';

  try {

    const loadedCharacter = await api.get(String(route.params.id));
    character.value = loadedCharacter;
    if (props.isDirector) {
      try { editorEmails.value = await api.characterEditors(String(route.params.id)); }
      catch (e: any) { editorError.value = e?.message || 'No se pudieron cargar los editores.'; }
    }
    if (route.query.mode === 'edit' && loadedCharacter.closed && (loadedCharacter.canEdit || props.isDirector)) {
      try {
        character.value = await api.edit(String(route.params.id));
      } catch (e: any) {
        closeError.value = e?.message || 'No se pudo abrir la ficha para edición.';
        await router.replace({ query: queryWithEditMode(false) });
      }
    } else if (route.query.mode === 'edit' && !loadedCharacter.canEdit && !props.isDirector) {
      await router.replace({ query: queryWithEditMode(false) });
    }
    await loadTraining();
    await loadOtherInventory(); await loadWeapons(); await loadProtectiveEquipment(); await loadAmmunition();
    legacyDraft.value = false; legacyEvolutionPoints.value = null;
    editing.value = !character.value?.closed && canEdit.value;
    if (editing.value) startModifierDraft();
    if (editing.value && route.query.mode !== 'edit') await router.replace({ query: queryWithEditMode(true) });

    if (character.value?.campaignId) {
      campaign.value = await api.campaign(character.value.campaignId);
      if (props.isDirector) {
        try { campaignMembers.value = await api.campaignMembers(character.value.campaignId) as CampaignMember[]; }
        catch (e: any) { editorError.value = e?.message || 'No se pudieron cargar los miembros de la campaña.'; }
      }
    }

  } catch (e: any) { error.value = e?.message || 'No se pudo cargar el personaje.'; }

  finally { loading.value = false; }

}

async function loadOtherInventory() {
  if (!route.params.id) return;
  inventoryLoading.value = true; inventoryError.value = '';
  try { otherInventory.value = await api.otherInventory(String(route.params.id)); }
  catch (e: any) { inventoryError.value = e?.message || 'No se pudo cargar el inventario.'; }
  finally { inventoryLoading.value = false; }
}
async function loadAmmunition() {
  if (!route.params.id) return;
  try {
    const [items, calibers] = await Promise.all([api.ammunition(String(route.params.id)), api.ammunitionCalibers(String(route.params.id))]);
    ammunition.value = items;
    ammunitionCalibers.value = calibers;
  } catch (e: any) { inventoryError.value = e?.message || 'No se pudo cargar la munición.'; }
}
async function loadWeapons() { if (!route.params.id) return; try { weapons.value = await api.weapons(String(route.params.id)); } catch (e:any) { inventoryError.value = e?.message || 'No se pudieron cargar las armas.'; } }
async function loadProtectiveEquipment() { if (!route.params.id) return; try { armors.value=await api.armors(String(route.params.id)); shields.value=await api.shields(String(route.params.id)); physicalShields.value=await api.physicalShields(String(route.params.id)); } catch(e:any){ inventoryError.value=e?.message||'No se pudo cargar las protecciones.'; } }
function armorAtSlot(slot:ArmorSlot){ return armors.value.find(item => item.slots.includes(slot)); }
function openArmorDetail(a?:Armor, slot?:ArmorSlot){ selectedArmor.value=a||null; armorDraft.value=a?{...a,slots:[...a.slots],rdBySlot:{...a.rdBySlot},armorBySlot:{...a.armorBySlot}}:{name:'',description:'',slots:slot?[slot]:[],rdBySlot:{HEAD:0,BODY:0,LEGS:0,ARMS:0},armorBySlot:{HEAD:0,BODY:0,LEGS:0,ARMS:0},imageUrl:null}; if(a){showArmorDetailModal.value=true;sheetView.value='inventory';}else{sheetView.value='armor-detail';} }
function closeArmorDetailModal(){showArmorDetailModal.value=false;selectedArmor.value=null;sheetView.value='inventory';}
function editSelectedArmor(){if(!selectedArmor.value)return;showArmorDetailModal.value=false;sheetView.value='armor-detail';}
function openShieldDetail(s?:Shield){ selectedShield.value=s||null; shieldDraft.value=s?{...s}:{name:'',description:'',hitPoints:0,imageUrl:null}; if(s){showShieldDetailModal.value=true;sheetView.value='inventory';}else{sheetView.value='shield-detail';} }
function closeShieldDetailModal(){showShieldDetailModal.value=false;selectedShield.value=null;sheetView.value='inventory';}
function editSelectedShield(){if(!selectedShield.value)return;showShieldDetailModal.value=false;sheetView.value='shield-detail';}
function openPhysicalShieldDetail(s?:PhysicalShield){selectedPhysicalShield.value=s||null;physicalShieldDraft.value=s?{...s}:{name:'',description:'',rd:0,armor:0,defense:0,otherEffects:'',imageUrl:null};if(s){showPhysicalShieldDetailModal.value=true;sheetView.value='inventory';}else{sheetView.value='physical-shield-detail';}}
function closePhysicalShieldDetailModal(){showPhysicalShieldDetailModal.value=false;selectedPhysicalShield.value=null;sheetView.value='inventory';}
function editSelectedPhysicalShield(){if(!selectedPhysicalShield.value)return;showPhysicalShieldDetailModal.value=false;sheetView.value='physical-shield-detail';}
function armorSlotUnavailable(slot:ArmorSlot){ return occupiedArmorSlots.value.has(slot) && !armorDraft.value.slots.includes(slot); }
function toggleArmorSlot(slot:ArmorSlot){ if(armorSlotUnavailable(slot)) return; const slots=armorDraft.value.slots; armorDraft.value.slots=slots.includes(slot)?slots.filter(x=>x!==slot):[...slots,slot]; }
async function onProtectiveImage(event:Event,target:'armor'|'shield'|'physicalShield'){const file=(event.target as HTMLInputElement).files?.[0];if(!file)return;if(!file.type.startsWith('image/')||file.size>5_000_000){inventoryError.value='Selecciona una imagen de hasta 5 MB.';return;}const url=await new Promise<string>((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result));reader.onerror=reject;reader.readAsDataURL(file);});if(target==='armor')armorDraft.value.imageUrl=url;else if(target==='shield')shieldDraft.value.imageUrl=url;else physicalShieldDraft.value.imageUrl=url;}
async function saveArmor(){ if(!route.params.id||!armorDraft.value.name.trim()||!armorDraft.value.slots.length)return; armorSaving.value=true;inventoryError.value='';try{const b={...armorDraft.value,name:armorDraft.value.name.trim(),slots:Object.fromEntries(armorDraft.value.slots.map(s=>[s,{rd:Number(armorDraft.value.rdBySlot[s]||0),armor:Number(armorDraft.value.armorBySlot[s]||0)}]))};if(selectedArmor.value)await api.updateArmor(String(route.params.id),selectedArmor.value.id,b);else await api.createArmorInventory(String(route.params.id),b);await loadProtectiveEquipment();sheetView.value='inventory';}catch(e:any){inventoryError.value=e?.message||'No se pudo guardar la armadura.';}finally{armorSaving.value=false;}}
async function saveShield(){if(!route.params.id||!shieldDraft.value.name.trim())return;shieldSaving.value=true;inventoryError.value='';try{const b={...shieldDraft.value,name:shieldDraft.value.name.trim(),hitPoints:Number(shieldDraft.value.hitPoints)};if(selectedShield.value)await api.updateShield(String(route.params.id),selectedShield.value.id,b);else await api.createShieldInventory(String(route.params.id),b);await loadProtectiveEquipment();sheetView.value='inventory';}catch(e:any){inventoryError.value=e?.message||'No se pudo guardar el escudo.';}finally{shieldSaving.value=false;}}
async function savePhysicalShield(){if(!route.params.id||!physicalShieldDraft.value.name.trim())return;shieldSaving.value=true;inventoryError.value='';try{const b={...physicalShieldDraft.value,name:physicalShieldDraft.value.name.trim(),rd:Number(physicalShieldDraft.value.rd),armor:Number(physicalShieldDraft.value.armor),defense:Number(physicalShieldDraft.value.defense)};if(selectedPhysicalShield.value)await api.updatePhysicalShield(String(route.params.id),selectedPhysicalShield.value.id,b);else await api.createPhysicalShieldInventory(String(route.params.id),b);await loadProtectiveEquipment();sheetView.value='inventory';}catch(e:any){inventoryError.value=e?.message||'No se pudo guardar el escudo.';}finally{shieldSaving.value=false;}}
async function deleteArmor(){if(!route.params.id||!selectedArmor.value||!confirm(`¿Eliminar ${selectedArmor.value.name}?`))return;await api.deleteArmor(String(route.params.id),selectedArmor.value.id);await loadProtectiveEquipment();closeArmorDetailModal();}
async function deleteShield(){if(!route.params.id||!selectedShield.value||!confirm(`¿Eliminar ${selectedShield.value.name}?`))return;await api.deleteShield(String(route.params.id),selectedShield.value.id);await loadProtectiveEquipment();closeShieldDetailModal();}
async function deletePhysicalShield(){if(!route.params.id||!selectedPhysicalShield.value||!confirm(`¿Eliminar ${selectedPhysicalShield.value.name}?`))return;await api.deletePhysicalShield(String(route.params.id),selectedPhysicalShield.value.id);await loadProtectiveEquipment();closePhysicalShieldDetailModal();}
function weaponAt(slot:string){ return weapons.value.find(w => w.slot === slot); }
function weaponImage(weapon: Weapon | WeaponCatalogItem){ const url=weapon.imageUrl?.startsWith('/weapons/') && 'catalogWeaponId' in weapon && weapon.catalogWeaponId ? `/api/weapon-catalog/${encodeURIComponent(weapon.catalogWeaponId)}/image` : weapon.imageUrl || undefined; return url?.startsWith('/api/weapon-catalog/') ? `${url}?v=3` : url; }
function weaponCatalogImage(weapon: WeaponCatalogItem){ return weapon.imageUrl?.startsWith('/api/weapon-catalog/') ? `${weapon.imageUrl}?v=3` : weapon.imageUrl || undefined; }
function weaponDamage(weapon: Weapon | WeaponCatalogItem){ return [weapon.damageVital, weapon.damageNormal, weapon.damageLight, weapon.damageVeryLight].join('/'); }
function weaponRate(rate: string | null | undefined){ const value=String(rate ?? '').trim(); return value ? (value.toLowerCase().includes('x') ? value : `x${value}`) : '—'; }
function numericWeaponValue(value: string | number | null | undefined){ const match=String(value ?? '').match(/\d+(?:[.,]\d+)?/); return match ? Math.max(0, Math.floor(Number(match[0].replace(',', '.')))) : 0; }
function weaponCadence(weapon: Weapon){ return Math.max(1, numericWeaponValue(weapon.rate)); }
function weaponAutomaticShots(weapon: Weapon){ return numericWeaponValue(weapon.automaticFire); }
function shootOptions(weapon: Weapon){
  const maximum=Math.min(weaponCadence(weapon), weapon.loadedBullets);
  const options=Array.from({length: maximum}, (_, index) => ({label:`${index + 1} disparo${index ? 's' : ''}`, shots:index + 1, automatic:false}));
  const automaticShots=weaponCadence(weapon) * weaponAutomaticShots(weapon);
  if(weaponAutomaticShots(weapon) > 0 && weapon.loadedBullets >= automaticShots) options.push({label:'Automático', shots:automaticShots, automatic:true});
  return options;
}
function emptyWeaponDraft(slot='SMALL_1'): Omit<Weapon,'id'> { return {slot,name:'',weaponType:'PISTOLA',size:'PEQUENA',range:0,reload:0,rate:'',damageVital:0,damageNormal:0,damageLight:0,damageVeryLight:0,aim:null,automaticFire:'',capacity:0,loadedBullets:0,caliber:'',extraRule:''}; }
function compatibleSlots(size:string){ return weaponSlots.filter(s => size === 'PEQUENA' || (size === 'MEDIANA' ? s.kind !== 'small' : s.kind === 'any')).map(s=>s.value); }
function compatibleSizes(slot:string){ return slot.startsWith('SMALL_') ? ['PEQUENA'] : slot.startsWith('MEDIUM_') ? ['PEQUENA','MEDIANA'] : weaponSizes.map(s=>s.value); }
function weaponMoveTargets(w:Weapon){ return compatibleSlots(w.size).filter(targetSlot => { const targetWeapon=weaponAt(targetSlot); return !targetWeapon || compatibleSlots(targetWeapon.size).includes(w.slot); }); }
function onWeaponSlotChange(){ if(!compatibleSizes(weaponDraft.value.slot).includes(weaponDraft.value.size)) weaponDraft.value.size=compatibleSizes(weaponDraft.value.slot)[0]; }
function onWeaponSizeChange(){ if(!compatibleSlots(weaponDraft.value.size).includes(weaponDraft.value.slot)) weaponDraft.value.slot=compatibleSlots(weaponDraft.value.size)[0]; }
function openNewWeapon(slot?:string){ selectedWeapon.value=null; weaponSlotLocked.value=slot !== undefined; catalogSlot.value=slot ?? 'SMALL_1'; weaponDraft.value=emptyWeaponDraft(catalogSlot.value); customImageUrl.value=null; sheetView.value='weapon-choice'; }
async function loadWeaponCatalog(){ catalogLoading.value=true; inventoryError.value=''; try { catalogWeapons.value=await api.weaponCatalog(catalogSlot.value,catalogSearch.value,catalogType.value); } catch(e:any) { inventoryError.value=e?.message || 'No se pudo cargar el catálogo de armas.'; } finally { catalogLoading.value=false; } }
function openWeaponCatalog(){ sheetView.value='weapon-catalog'; loadWeaponCatalog(); }
async function selectCatalogWeapon(item:WeaponCatalogItem){ selectedCatalogWeapon.value=item; showCatalogWeaponModal.value=true; }
async function addSelectedCatalogWeapon(){ if(!route.params.id || !selectedCatalogWeapon.value) return; catalogLoading.value=true; inventoryError.value=''; try { await api.addCatalogWeaponToCharacter(selectedCatalogWeapon.value.id,String(route.params.id),catalogSlot.value); await loadWeapons(); showCatalogWeaponModal.value=false; selectedCatalogWeapon.value=null; sheetView.value='inventory'; } catch(e:any) { inventoryError.value=e?.message || 'No se pudo añadir el arma seleccionada.'; } finally { catalogLoading.value=false; } }
async function onCustomWeaponImage(event:Event){ const file=(event.target as HTMLInputElement).files?.[0]; if(!file) return; if(!file.type.startsWith('image/') || file.size>5_000_000){ inventoryError.value='Selecciona una imagen de hasta 5 MB.'; return; } customImageUrl.value=await new Promise<string>((resolve,reject)=>{ const reader=new FileReader(); reader.onload=()=>resolve(String(reader.result)); reader.onerror=reject; reader.readAsDataURL(file); }); }
function openInventoryType(){ sheetView.value='inventory-type'; }
function openWeapon(w:Weapon){ selectedWeapon.value=w; weaponSlotLocked.value=true; weaponEditMode.value=false; weaponDraft.value={...w}; showWeaponDetailModal.value=true; }
function closeWeaponDetail(){showWeaponDetailModal.value=false; selectedWeapon.value=null; weaponEditMode.value=false; weaponSlotLocked.value=false; sheetView.value='inventory';}
function editSelectedWeapon(){ if(!selectedWeapon.value) return; weaponEditMode.value=true; showWeaponDetailModal.value=false; sheetView.value='weapon-detail'; }
async function reloadWeapon(weapon: Weapon){
  if(!route.params.id || weaponReloading.value) return;
  if(weapon.loadedBullets >= weapon.capacity && !confirm('El cargador ya contiene todas las balas. ¿Quieres recargarlo de nuevo?')) return;
  weaponReloading.value=true; inventoryError.value='';
  try {
    const result = await api.reloadWeapon(String(route.params.id), weapon.id);
    await loadWeapons();
    await loadAmmunition();
    inventoryError.value = result.missing > 0
      ? `Recarga parcial: se consumieron ${result.consumed} balas y faltan ${result.missing} balas.`
      : '';
  }
  catch(e:any){ inventoryError.value=e?.message||'No se pudo recargar el arma.'; }
  finally { weaponReloading.value=false; }
}
function openShoot(weapon: Weapon){ if(weapon.loadedBullets <= 0) return; if(weaponCadence(weapon) <= 1){ shootWeapon(weapon, 1); return; } shootWeaponTarget.value=weapon; showShootModal.value=true; inventoryError.value=''; }
function closeShoot(){ showShootModal.value=false; shootWeaponTarget.value=null; }
async function shootWeapon(weapon: Weapon, shots: number, automatic=false){
  if(!route.params.id || weaponShooting.value) return;
  weaponShooting.value=true; inventoryError.value='';
  try { await api.shootWeapon(String(route.params.id), weapon.id, shots, automatic); await loadWeapons(); closeShoot(); openWeaponAimRolls(weapon, automatic ? weaponCadence(weapon) : shots); }
  catch(e:any){ inventoryError.value=e?.message||'No se pudo disparar el arma.'; }
  finally { weaponShooting.value=false; }
}
function ammunitionForCaliber(caliber: string | null | undefined){
  if(!caliber) return 0;
  return ammunition.value.find(item => item.caliber === caliber)?.quantity ?? 0;
}
async function saveWeapon(){ if(!route.params.id || !weaponDraft.value.name.trim() || !weaponDraft.value.caliber.trim() || !weaponDraft.value.rate.trim()) return; weaponSaving.value=true; inventoryError.value=''; try { const body={...weaponDraft.value,name:weaponDraft.value.name.trim(),caliber:weaponDraft.value.caliber.trim(),rate:weaponDraft.value.rate.trim(),range:Number(weaponDraft.value.range),reload:Number(weaponDraft.value.reload),damageVital:Number(weaponDraft.value.damageVital),damageNormal:Number(weaponDraft.value.damageNormal),damageLight:Number(weaponDraft.value.damageLight),damageVeryLight:Number(weaponDraft.value.damageVeryLight),aim:weaponDraft.value.aim==null?null:Number(weaponDraft.value.aim),capacity:Number(weaponDraft.value.capacity),loadedBullets:Number(weaponDraft.value.loadedBullets)}; if(selectedWeapon.value) await api.updateWeapon(String(route.params.id),selectedWeapon.value.id,body); else { const { loadedBullets: _loadedBullets, ...catalogBody } = body; const catalog=await api.createCatalogWeapon({...catalogBody,imageUrl:customImageUrl.value}); await api.addCatalogWeaponToCharacter(catalog.id,String(route.params.id),weaponDraft.value.slot); } await loadWeapons(); closeWeaponDetail(); } catch(e:any){inventoryError.value=e?.message||'No se pudo guardar el arma.';} finally{weaponSaving.value=false;} }
async function deleteWeapon(){if(!route.params.id||!selectedWeapon.value||!confirm(`¿Eliminar ${selectedWeapon.value.name}?`))return;weaponDeleting.value=true;try{await api.deleteWeapon(String(route.params.id),selectedWeapon.value.id);await loadWeapons();closeWeaponDetail();}catch(e:any){inventoryError.value=e?.message||'No se pudo eliminar el arma.';}finally{weaponDeleting.value=false;}}
async function moveWeapon(w:Weapon,slot:string){if(slot===w.slot||!route.params.id)return;if(!weaponMoveTargets(w).includes(slot)){inventoryError.value='El intercambio no es válido: ambas armas deben poder entrar en el hueco de la otra.';return;}weaponMoving.value=true;try{const moved=await api.moveWeapon(String(route.params.id),w.id,slot);if(selectedWeapon.value?.id===w.id){selectedWeapon.value=moved;weaponDraft.value={...moved};}await loadWeapons();}catch(e:any){inventoryError.value=e?.message||'El arma no cabe en ese hueco.';}finally{weaponMoving.value=false;}}
function emptyInventoryDraft(): Omit<OtherInventoryItem, 'id'> { return { name: '', description: '', location: '', quantity: 1, unitValue: 0 }; }
function openInventory() { navigateToSection('inventory'); }
function emptyAmmunitionDraft(): Omit<Ammunition, 'id'> { return { caliber: ammunitionCalibers.value[0] || '', quantity: 1 }; }
function openNewAmmunition() { selectedAmmunition.value = null; ammunitionDraft.value = emptyAmmunitionDraft(); sheetView.value = 'ammunition-detail'; }
function openAmmunition(item: Ammunition) { selectedAmmunition.value = item; ammunitionDraft.value = { caliber: item.caliber, quantity: item.quantity }; sheetView.value = 'ammunition-detail'; }
function closeAmmunitionDetail() { selectedAmmunition.value = null; sheetView.value = 'inventory'; }
async function saveAmmunition() {
  if (!route.params.id || !ammunitionDraft.value.caliber || Number(ammunitionDraft.value.quantity) < 1) return;
  ammunitionSaving.value = true; inventoryError.value = '';
  try {
    const body = { caliber: ammunitionDraft.value.caliber, quantity: Number(ammunitionDraft.value.quantity) };
    if (selectedAmmunition.value) await api.updateAmmunition(String(route.params.id), selectedAmmunition.value.id, body);
    else await api.createAmmunition(String(route.params.id), body);
    await loadAmmunition(); closeAmmunitionDetail();
  } catch (e: any) { inventoryError.value = e?.message || 'No se pudo guardar la munición.'; }
  finally { ammunitionSaving.value = false; }
}
async function decrementAmmunition(item: Ammunition, amount: -1 | -5 | -10) {
  if (!route.params.id || item.quantity < Math.abs(amount)) return;
  ammunitionDeleting.value = true; inventoryError.value = '';
  try { await api.decrementAmmunition(String(route.params.id), item.id, amount); await loadAmmunition(); }
  catch (e: any) { inventoryError.value = e?.message || 'No se pudo descontar la munición.'; }
  finally { ammunitionDeleting.value = false; }
}
function openNewOtherItem() { selectedOtherItem.value = null; inventoryDraft.value = emptyInventoryDraft(); sheetView.value = 'inventory-detail'; }
function openOtherItem(item: OtherInventoryItem) { selectedOtherItem.value = item; inventoryDraft.value = { name: item.name, description: item.description || '', location: item.location || '', quantity: item.quantity, unitValue: item.unitValue ?? 0 }; sheetView.value = 'inventory-detail'; }
function closeInventoryDetail() { selectedOtherItem.value = null; sheetView.value = 'inventory'; }
async function saveOtherItem() {
  if (!route.params.id || !inventoryDraft.value.name.trim() || Number(inventoryDraft.value.quantity) < 1) return;
  inventorySaving.value = true; inventoryError.value = '';
  try {
    const body = { ...inventoryDraft.value, name: inventoryDraft.value.name.trim(), quantity: Number(inventoryDraft.value.quantity), unitValue: inventoryDraft.value.unitValue == null ? 0 : Number(inventoryDraft.value.unitValue) };
    if (selectedOtherItem.value) await api.updateOtherInventory(String(route.params.id), selectedOtherItem.value.id, body);
    else await api.createOtherInventory(String(route.params.id), body);
    await loadOtherInventory(); closeInventoryDetail();
  } catch (e: any) { inventoryError.value = e?.message || 'No se pudo guardar el objeto.'; }
  finally { inventorySaving.value = false; }
}
async function deleteOtherItem() {
  if (!route.params.id || !selectedOtherItem.value || !window.confirm(`¿Eliminar ${selectedOtherItem.value.name}?`)) return;
  inventoryDeleting.value = true; inventoryError.value = '';
  try { await api.deleteOtherInventory(String(route.params.id), selectedOtherItem.value.id); await loadOtherInventory(); closeInventoryDetail(); }
  catch (e: any) { inventoryError.value = e?.message || 'No se pudo eliminar el objeto.'; }
  finally { inventoryDeleting.value = false; }
}

async function loadTraining() { if (!route.params.id) return; trainingLoading.value=true; try { trainingData.value=await api.training(String(route.params.id)) as typeof trainingData.value; } catch(e:any){ trainingError.value=e?.message||'No se pudo cargar la trayectoria'; } finally { trainingLoading.value=false; } }
function trainingPayload() { const course=trainingDraft.value.type==='COURSE'; return {...trainingDraft.value,startAge:course?trainingData.value.startingAge:Number(trainingDraft.value.startAge),endAge:course?trainingData.value.sheetAge:Number(trainingDraft.value.endAge),priority:Number(trainingDraft.value.priority||0),primaryAttribute:trainingDraft.value.primaryAttribute||null,secondaryAttribute:course?null:(trainingDraft.value.secondaryAttribute||null),tertiaryAttribute:course?null:(trainingDraft.value.tertiaryAttribute||null)}; }
async function refreshTrainingPreview() { if (!route.params.id || !trainingDraft.value.name || !trainingDraft.value.type || trainingDraft.value.startAge == null || trainingDraft.value.endAge == null || Number(trainingDraft.value.endAge) <= Number(trainingDraft.value.startAge)) { trainingPreview.value=null; return; } try { trainingPreview.value=await api.previewTraining(String(route.params.id), {activity:trainingPayload(), replacingActivityId:trainingEditingId.value}) as TrainingPreview; } catch { trainingPreview.value=null; } }
watch([trainingDraft, trainingEditingId], () => { if (trainingPreviewTimer) clearTimeout(trainingPreviewTimer); trainingPreviewTimer=setTimeout(refreshTrainingPreview, 200); }, {deep:true});
watch(() => trainingDraft.value.startAge, () => { const options=trainingEndAgeOptions.value; if (options.length && !options.includes(Number(trainingDraft.value.endAge))) trainingDraft.value.endAge=options[0]; });
watch(() => trainingDraft.value.type, type => {
  if (type === 'COURSE') {
    trainingDraft.value.startAge = trainingData.value.startingAge;
    trainingDraft.value.endAge = trainingData.value.sheetAge;
    trainingDraft.value.secondaryAttribute = '';
    trainingDraft.value.tertiaryAttribute = '';
    return;
  }

  const options = trainingEndAgeOptions.value;
  if (options.length && !options.includes(Number(trainingDraft.value.endAge))) trainingDraft.value.endAge = options[0];
});
function trainingDefaultInterval(){const activities=trainingData.value.activities.filter(a=>a.type!=='COURSE'&&!(a.type==='OCCUPATION'&&a.concurrent));for(let start=trainingData.value.startingAge;start<trainingData.value.sheetAge;start++){for(let end=start+1;end<=trainingData.value.sheetAge;end++){if(!activities.some(a=>start<a.endAge&&a.startAge<end))return{startAge:start,endAge:end};}}return{startAge:trainingData.value.startingAge,endAge:Math.min(trainingData.value.startingAge+1,trainingData.value.sheetAge)};}
function resetTrainingDraft(){trainingEditingId.value=null;trainingPreview.value=null;trainingDraft.value={type:'FORMATION',name:'',...trainingDefaultInterval(),priority:0,concurrent:false,primaryAttribute:'',secondaryAttribute:'',tertiaryAttribute:''};}
function openTraining() { resetTrainingDraft(); showTrainingForm.value=true; showTrainingModal.value=true; }
function editTraining(a:TrainingActivity){trainingPreview.value=null;trainingEditingId.value=a.id;trainingDraft.value={...a};showTrainingForm.value=true;}
async function saveTraining(){if(!route.params.id||!trainingDraft.value.name)return;trainingLoading.value=true;trainingError.value='';try{const b=trainingPayload();if(trainingEditingId.value)await api.updateTraining(String(route.params.id),trainingEditingId.value,b);else await api.addTraining(String(route.params.id),b);await loadTraining();character.value=await api.get(String(route.params.id));resetTrainingDraft();showTrainingForm.value=false;}catch(e:any){trainingError.value=e?.message||'No se pudo guardar';}finally{trainingLoading.value=false;}}
async function removeTraining(a:TrainingActivity){if(!route.params.id||!window.confirm(`¿Eliminar ${a.name}?`))return;trainingLoading.value=true;try{await api.deleteTraining(String(route.params.id),a.id);await loadTraining();character.value=await api.get(String(route.params.id));}catch(e:any){trainingError.value=e?.message||'No se pudo eliminar';}finally{trainingLoading.value=false;}}
function trainingTypeLabel(t:string){return ({FORMATION:'Formación',PROFESSION:'Profesión',OCCUPATION:'Ocupación',COURSE:'Curso'} as Record<string,string>)[t]||t;}
function trainingBarStyle(a:TrainingActivity){const span=Math.max(1,trainingData.value.sheetAge-trainingData.value.startingAge);return{left:`${((a.startAge-trainingData.value.startingAge)/span)*100}%`,width:`${((a.endAge-a.startAge)/span)*100}%`};}
function trainingAttributeDisabled(key:string, slot:'primary'|'secondary'|'tertiary'){const duplicate=[trainingDraft.value.primaryAttribute,trainingDraft.value.secondaryAttribute,trainingDraft.value.tertiaryAttribute].some((selected, index) => selected===key && ['primary','secondary','tertiary'][index]!==slot); const trajectoryTotal=(character.value?.attributeModifiers?.[key]??[]).filter(modifier=>modifier.source?.startsWith('TRAINING:')).reduce((sum,modifier)=>sum+Number(modifier.value||0),0); return duplicate || (trainingDraft.value.type==='COURSE' && trajectoryTotal>=5 && trainingDraft.value.primaryAttribute!==key);}
function trainingGroup(a:TrainingActivity){return trainingData.value.activities.filter(item=>item.startAge===a.startAge);}
function canMoveTraining(a:TrainingActivity, direction:number){const group=trainingGroup(a);const index=group.findIndex(item=>item.id===a.id);return index>=0 && index+direction>=0 && index+direction<group.length;}
async function moveTraining(a:TrainingActivity, direction:number){if(!route.params.id||!canMoveTraining(a,direction))return;const group=trainingGroup(a);const index=group.findIndex(item=>item.id===a.id);const reordered=[...group];const [moved]=reordered.splice(index,1);reordered.splice(index+direction,0,moved);trainingReordering.value=a.id;trainingError.value='';try{await api.reorderTraining(String(route.params.id),reordered.map(item=>item.id));await loadTraining();character.value=await api.get(String(route.params.id));}catch(e:any){trainingError.value=e?.message||'No se pudo reordenar';}finally{trainingReordering.value=null;}}

const showProfileModal=ref(false); const showEditorsModal=ref(false); const showMinorModal=ref(false); const minorKind=ref('GALDR'); const customName=ref(''); const customFormula=ref(''); const customSource=ref('informatica'); const minorBusy=ref(false); const minorError=ref('');

function insertToken(t:string){ customFormula.value += t; }

function clearFormula(){customFormula.value='';}

async function addMinor(){ if(!campaign.value||!customName.value&&minorKind.value==='CUSTOM') return; minorBusy.value=true; minorError.value=''; try { const presets:any={GALDR:{key:'galdr',name:'Galdr',maxFormula:'min(cruzarbifrost,einherjer,sentiryggdrasil)',bonusSource:null,type:'GALDR'},ASTRONAVEGAR:{key:'astronavegar',name:'Astronavegar',maxFormula:'conduccion',bonusSource:'conduccion',type:'PRESET'},FORJA:{key:'forja',name:'Forja',maxFormula:'(fisico+estudio)/2',bonusSource:'informatica',type:'PRESET'}}; const body=minorKind.value==='CUSTOM'?{name:customName.value,maxFormula:customFormula.value,bonusSource:customSource.value,type:'CUSTOM'}:presets[minorKind.value]; await api.createMinorAttribute(campaign.value.id,body); character.value=await api.get(String(route.params.id)); showMinorModal.value=false; } catch(e:any){minorError.value=e?.message||'No se pudo crear';} finally{minorBusy.value=false;} }

function startModifierDraft() {
  if (!character.value) return;
  modifierDraft.value = Object.fromEntries(Object.entries(character.value.attributeModifiers ?? {}).map(([key, values]) => [key, values.filter(value => !value.source || value.source === 'MANUAL').map(value => ({ ...value }))]));
}

async function startEdit() {
  if (!character.value || editing.value || !canEdit.value) return;
  try {
    character.value = await api.edit(String(route.params.id));
    legacyDraft.value = false; legacyEvolutionPoints.value = null;
    editing.value = true;
    startModifierDraft();
    closeError.value = '';
    await router.push({ query: queryWithEditMode(true) });
  } catch (e: any) { closeError.value = e?.message || 'No se pudo abrir la ficha para edición.'; }
}

async function toggleEditor(email: string) {
  if (!props.isDirector || !character.value || editorBusy.value) return;
  editorBusy.value = true; editorError.value = '';
  try {
    editorEmails.value = editorEmails.value.some(item => item.toLowerCase() === email.toLowerCase())
      ? await api.removeCharacterEditor(character.value.id, email)
      : await api.addCharacterEditor(character.value.id, email);
  } catch (e: any) { editorError.value = e?.message || 'No se pudo actualizar el permiso de edición.'; }
  finally { editorBusy.value = false; }
}

function hasEditorAccess(email: string) {
  return editorEmails.value.some(item => item.toLowerCase() === email.toLowerCase());
}

async function openAttributeDetail(key: string) {
  detailError.value = '';
  detailLoading.value = true;
  showAttributeDetail.value = true;
  attributeDetail.value = null;
  try {
    attributeDetail.value = await api.attributeDetail(String(route.params.id), key);
    if (attributeDetail.value?.type === 'DERIVED' && editing.value) {
      attributeDetail.value = { ...attributeDetail.value, calculatedValue: derivedBaseValue(key) };
    }
  }
  catch (e: any) { detailError.value = e?.message || 'No se pudo cargar el atributo.'; }
  finally { detailLoading.value = false; }
}

async function addExperience() {
  const amount = Number(experienceAmount.value);
  if (!Number.isInteger(amount) || amount < 1) { experienceError.value = 'Introduce una cantidad entera positiva.'; return; }
  experienceBusy.value = true; experienceError.value = '';
  try { character.value = await api.addExperience(String(route.params.id), amount); experienceAmount.value = null; showExperienceModal.value = false; }
  catch (e: any) { experienceError.value = e?.message || 'No se pudo añadir experiencia.'; }
  finally { experienceBusy.value = false; }
}

function modifierRows(key: string): AttributeModifier[] {
  return modifierDraft.value[key] ?? (modifierDraft.value[key] = []);
}

function isTrainingModifier(modifier: AttributeModifier) { return modifier.source?.startsWith('TRAINING:') ?? false; }
function modifierTotal(values: AttributeModifier[]) { return values.reduce((sum, modifier) => sum + (Number(modifier.value) || 0), 0); }
function modifierDisplayName(name: string): string {
  const labels: Record<string, string> = { FORMATION: 'Formación', PROFESSION: 'Profesión', OCCUPATION: 'Ocupación', COURSE: 'Curso' };
  const separator = name.indexOf(':');
  if (separator <= 0) return name;
  const type = name.slice(0, separator).trim().toUpperCase();
  return labels[type] ? `${labels[type]}:${name.slice(separator + 1)}` : name;
}

function detailModifierTotal(): number {
  if (!attributeDetail.value) return 0;
  if (!editing.value) return modifierTotal(attributeDetail.value.modifiers);
  const trajectoryTotal = modifierTotal(attributeDetail.value.modifiers.filter(isTrainingModifier));
  return trajectoryTotal + modifierTotal(modifierRows(attributeDetail.value.key));
}

function detailTotal(): number {
  if (!attributeDetail.value) return 0;
  if (attributeDetail.value.type === 'DERIVED') return derivedBaseValue(attributeDetail.value.key) + detailModifierTotal();
  return attributeDetail.value.total - modifierTotal(attributeDetail.value.modifiers) + detailModifierTotal();
}

function detailBonus(kind: 'plusOne' | 'plusD6'): number {
  if (!attributeDetail.value) return 0;
  const total = detailTotal();
  if (attributeDetail.value.type === 'GALDR') return kind === 'plusOne' ? Math.floor(total / 5) : Math.floor(total / 3);
  return kind === 'plusOne'
    ? oneBonus(attributeDetail.value.key, total, majorKeys.includes(attributeDetail.value.key))
    : d6Bonus(attributeDetail.value.key, total, majorKeys.includes(attributeDetail.value.key));
}

function derivedBaseValue(key: string): number {
  const valueOf = (source: string) => displayedAttributeTotal(source, attributes.value[source] ?? 0);
  if (key === 'vida') {
    const fisico = valueOf('fisico');
    return fisico <= 15 ? 70 + fisico * 5 : 70 + 15 * 5 + Math.floor((fisico - 15) * 2.5);
  }
  if (key === 'bifrost') {
    const mente = valueOf('mente');
    return mente * 10;
  }
  const esquivaPlusOne = oneBonus('esquiva', valueOf('esquiva'), false);
  if (key === 'defensaCuerpo') return 10 + esquivaPlusOne + oneBonus('destreza', valueOf('destreza'), false);
  if (key === 'defensaDistancia') return 15 + esquivaPlusOne;
  return 0;
}

function displayedAttributeTotal(key: string, fallback: number): number {
  if (derivedStatKeys.includes(key) && editing.value) {
    return derivedBaseValue(key) + modifierRows(key).reduce((sum, modifier) => sum + (Number(modifier.value) || 0), 0);
  }
  const persistedTotal = character.value?.attributeTotals?.[key] ?? fallback;
  if (!editing.value) return persistedTotal;
  const persistedModifiers = character.value?.attributeModifiers?.[key] ?? [];
  // The draft contains only manual modifiers. Never fall back to persisted
  // trajectory modifiers, otherwise training would be applied twice in edit mode.
  const draftModifiers = modifierDraft.value[key] ?? [];
  const persistedModifierTotal = modifierTotal(persistedModifiers.filter(modifier => !isTrainingModifier(modifier)));
  const draftModifierTotal = draftModifiers.reduce((sum, modifier) => sum + (Number(modifier.value) || 0), 0);
  return persistedTotal - persistedModifierTotal + draftModifierTotal;
}

function addModifier(key: string) { modifierRows(key).push({ name: '', value: 0 }); }

function removeModifier(key: string, index: number) { modifierRows(key).splice(index, 1); }

async function saveModifiers(): Promise<boolean> {
  if (!character.value || modifierSaveBusy.value) return false;
  modifierError.value = '';
  const body: Record<string, AttributeModifier[]> = {};
  for (const [key, values] of Object.entries(modifierDraft.value)) {
    const names = new Set<string>();
    for (const modifier of values) {
      modifier.name = modifier.name.trim();
      if (!modifier.name) { modifierError.value = 'Todos los modificadores deben tener nombre.'; return false; }
      if (names.has(modifier.name)) { modifierError.value = `El modificador «${modifier.name}» está repetido.`; return false; }
      names.add(modifier.name);
    }
    if (values.length) body[key] = values;
  }
  modifierSaveBusy.value = true;
  try {
    const result = await api.saveAttributeModifiers(String(route.params.id), body) as { character: Character };
    if (!legacyDraft.value) character.value = result.character;
    closeAttributeDetail();
    return true;
  } catch (e: any) { modifierError.value = e?.message || 'No se pudieron guardar los modificadores.'; return false; }
  finally { modifierSaveBusy.value = false; }
}

async function importLegacy() {
  if (!legacyCode.value.trim() || legacyBusy.value) return;
  legacyBusy.value = true; legacyError.value = '';
  try {
    const imported = await api.importLegacy(String(route.params.id), legacyCode.value) as {
      level:number; experience:number; evolutionPoints:number; attributes:Record<string,number>;
      genetics:Record<string,number>; extras:Record<string,number>;
    };
    if (!character.value) return;
    const totals = { ...imported.attributes };
    Object.entries(imported.extras).forEach(([key, value]) => { totals[key] = (totals[key] ?? 0) + value; });
    character.value = { ...character.value, level: imported.level, experience: imported.experience,
      attributes: imported.attributes, genetics: imported.genetics, attributeTotals: totals,
      minorAttributes: (character.value.minorAttributes ?? []).map(attribute => ({ ...attribute, value: 0, ranks: 0, total: 0 })),
      attributeModifiers: Object.fromEntries(Object.entries(imported.extras).map(([key, value]) => [key, [{ name: 'extra', value }]])), closed: false };
    modifierDraft.value = Object.fromEntries(Object.entries(imported.extras).map(([key, value]) => [key, [{ name: 'extra', value }]]));
    legacyEvolutionPoints.value = imported.evolutionPoints;
    legacyDraft.value = true;
    showLegacyImport.value = false; legacyCode.value = '';
  } catch (e: any) { legacyError.value = e?.message || 'No se pudo cargar el código legacy.'; }
  finally { legacyBusy.value = false; }
}

async function exportLegacy() {
  legacyError.value = '';
  try { legacyExportCode.value = await api.exportLegacy(String(route.params.id)); showLegacyExport.value = true; }
  catch (e: any) { legacyError.value = e?.message || 'No se pudo exportar el código legacy.'; }
}

async function closeDraft() {
  if (!character.value || !editing.value || closeBusy.value) return;
  closeBusy.value = true; closeError.value = '';
  try {
    if (!await saveModifiers()) { closeBusy.value = false; return; }
    const source = character.value;
    const result = await api.save(String(route.params.id), {
      name: source.name,
      level: source.level,
      experience: source.experience,
      attributes: source.attributes ?? {},
      genetics: source.genetics ?? source.genetic ?? {},
      minorAttributes: Object.fromEntries((source.minorAttributes ?? []).map(attribute => [attribute.key, attribute.value ?? attribute.ranks ?? 0])),
      visible: true,
       final: true,
       evolutionPoints: legacyEvolutionPoints.value,
       einherjer: true,
       awakened: source.awakened ?? false,
       einherjerOrigin: source.einherjerOrigin ?? null,
       startingAge: source.startingAge ?? null,
       awakeningAge: source.awakened ? (source.awakeningAge ?? null) : null,
       sheetAge: source.sheetAge ?? null,
     });
    character.value = result.character;
    editing.value = false;
    showProfileModal.value = false;
    legacyDraft.value = false; legacyEvolutionPoints.value = null;
    await router.replace({ query: queryWithEditMode(false) });
  } catch (e: any) { closeError.value = e?.message || 'No se pudo cerrar la ficha.'; }
  finally { closeBusy.value = false; }
}

async function cancelChanges() {
  if (!editing.value || cancelChangesBusy.value || !confirm('¿Cancelar todos los cambios y volver a la última versión cerrada?')) return;
  cancelChangesBusy.value = true; closeError.value = '';
  try {
    const result = await api.cancelChanges(String(route.params.id)) as { character: Character };
    character.value = result.character; editing.value = false; showProfileModal.value = false; legacyDraft.value = false; legacyEvolutionPoints.value = null; startModifierDraft();
    await router.replace({ query: queryWithEditMode(false) });
  } catch (e: any) { closeError.value = e?.message || 'No se pudieron cancelar los cambios.'; }
  finally { cancelChangesBusy.value = false; }
}

async function loadHistory() {
  historyLoading.value = true; historyError.value = '';
  try { history.value = await api.milestones(String(route.params.id)) as HistoryVersion[]; selectedHistory.value = history.value[0] ?? null; showHistory.value = true; }
  catch (e: any) { historyError.value = e?.message || 'No se pudo cargar el historial.'; }
  finally { historyLoading.value = false; }
}

function openHistory() {
  if (route.name !== 'character-history') router.push({ name: 'character-history', params: { id: String(route.params.id) }, query: { ...route.query } });
}

async function recoverHistory(version: HistoryVersion) {
  if (historyRecovering.value || !confirm(`¿Recuperar la versión de nivel ${version.level} del ${new Date(version.createdAt).toLocaleString('es-ES')}?`)) return;
  historyRecovering.value = version.id; historyError.value = '';
  try {
    const result = await api.recoverMilestone(String(route.params.id), version.id) as { character: Character };
    character.value = result.character; editing.value = false; legacyDraft.value = false; legacyEvolutionPoints.value = null; startModifierDraft();
    history.value = await api.milestones(String(route.params.id)) as HistoryVersion[]; selectedHistory.value = history.value[0] ?? null;
  } catch (e: any) { historyError.value = e?.message || 'No se pudo recuperar la versión.'; }
  finally { historyRecovering.value = ''; }
}

async function openLastUpgrade() { if (!character.value?.closed || lastUpgradeLoading.value) return; showLastUpgrade.value = true; currentUpgradeMode.value = false; lastUpgrade.value = null; lastUpgradeError.value = ''; lastUpgradeLoading.value = true; try { lastUpgrade.value = await api.lastUpgrade(String(route.params.id)) as LastUpgrade; } catch (e: any) { lastUpgradeError.value = e?.message || 'No se pudo comparar la última subida.'; } finally { lastUpgradeLoading.value = false; } }
async function openCurrentUpgrade() { if (!editing.value || lastUpgradeLoading.value) return; if (!await saveModifiers()) return; showLastUpgrade.value = true; currentUpgradeMode.value = true; lastUpgrade.value = null; lastUpgradeError.value = ''; lastUpgradeLoading.value = true; try { lastUpgrade.value = await api.currentUpgrade(String(route.params.id)) as LastUpgrade; } catch (e: any) { lastUpgradeError.value = e?.message || 'No se pudo comparar la subida actual.'; } finally { lastUpgradeLoading.value = false; } }
function closeLastUpgrade() { showLastUpgrade.value = false; currentUpgradeMode.value = false; lastUpgrade.value = null; lastUpgradeError.value = ''; }
function upgradeScoreLabel(change: { key:string; type:string }) { return change.type === 'genetic' ? geneticLabels[change.key] || change.key : attributeLabels[change.key] || customMinor(change.key)?.name || change.key; }

function draftFor(source: Character, targetLevel: number, targetExperience: number, evolutionAvailable: number, geneticsAvailable: number, minorEvolutionCost = source.allocation?.minorEvolutionCost ?? 5): AllocationDraft {
  const sourceAttributes = source.attributes ?? {};
  const sourceGenetics = source.genetics ?? source.genetic ?? {};
  const attributes: Record<string, number> = {};
  [...majorKeys, ...minorKeys].forEach(key => { attributes[key] = Math.max(0, sourceAttributes[key] ?? 0); });
  const genetics: Record<string, number> = {};
  geneticGroups.flatMap(group => group.keys).forEach(key => { genetics[key] = Math.max(0, sourceGenetics[key] ?? 0); });
  const minorAttributes: Record<string, number> = {};
  (source.minorAttributes ?? []).forEach(attribute => { minorAttributes[attribute.key] = Math.max(0, attribute.value ?? attribute.ranks ?? 0); });
  return { level: targetLevel, experience: targetExperience, evolutionAvailable, geneticsAvailable, minorEvolutionCost, attributes, genetics, minorAttributes,
    baseAttributes: { ...attributes }, baseGenetics: { ...genetics }, baseMinorAttributes: { ...minorAttributes } };
}

async function openLevelUp(all = false) {
  if (!character.value || (all ? !canLevelUpAll.value : !canLevelUp.value)) return;
  if (editing.value && !await saveModifiers()) return;
  const source = character.value;
  allocationMode.value = all ? 'all' : 'single';
  allocationStep.value = 1;
  allocationTotal.value = all ? Math.max(2, Math.floor(source.experience / 100)) : 1;
  const currentAllocation = source.allocation;
  const evolutionReward = source.allocation?.nextEvolutionReward ?? (35 + Math.max(0, source.attributes?.evolcurva ?? 0));
  const geneticsReward = source.allocation?.nextGeneticsReward ?? 3;
  allocationDraft.value = draftFor(source, source.level + 1, source.experience - 100,
    (currentAllocation?.evolutionRemaining ?? 0) + evolutionReward,
    (currentAllocation?.geneticsRemaining ?? 0) + geneticsReward);
  clampAllocationMinors();
  levelError.value = '';
  showAllocationModal.value = true;
}

function closeAllocation() { if (!levelBusy.value) { showAllocationModal.value = false; allocationDraft.value = null; } }

async function submitAllocation() {
  const draft = allocationDraft.value;
  if (!draft || !allocationValid.value) return;
  const finalStep = allocationMode.value === 'single' || allocationStep.value === allocationTotal.value;
  const payload: AllocationPayload = { ...draft, visible: false, final: false };
  levelBusy.value = true; levelError.value = '';
  let savedFinalStep = false;
  try {
    const result = allocationMode.value === 'single'
      ? await api.levelUp(String(route.params.id), payload)
      : await api.levelUpAll(String(route.params.id), payload);
    const updatedCharacter: Character = result.character;
    character.value = updatedCharacter;
    savedFinalStep = finalStep;
    if (!finalStep) {
      allocationStep.value += 1;
      allocationDraft.value = draftFor(updatedCharacter, updatedCharacter.level + 1, updatedCharacter.experience - 100,
        (updatedCharacter.allocation?.evolutionRemaining ?? 0) + (updatedCharacter.allocation?.nextEvolutionReward ?? (35 + Math.max(0, updatedCharacter.attributes?.evolcurva ?? 0))),
        (updatedCharacter.allocation?.geneticsRemaining ?? 0) + (updatedCharacter.allocation?.nextGeneticsReward ?? 3),
        updatedCharacter.allocation?.minorEvolutionCost ?? 5);
      clampAllocationMinors();
      await nextTick();
      allocationModal.value?.scrollTo({ top: 0 });
    }
  } catch (e: any) { levelError.value = e?.message || 'No se pudo guardar la asignación de nivel.'; }
  finally {
    levelBusy.value = false;
    if (savedFinalStep) closeAllocation();
  }
}

function closeAttributeDetail() { showAttributeDetail.value = false; attributeDetail.value = null; detailError.value = ''; }

async function deleteAttribute() {
  if (!attributeDetail.value?.deletable || !attributeDetail.value.definitionId) return;
  if (!window.confirm(`¿Eliminar el atributo «${attributeDetail.value.name}» de este personaje?`)) return;
  deletingAttribute.value = true; detailError.value = '';
  try {
    await api.deleteMinorAttribute(String(route.params.id), attributeDetail.value.definitionId);
    character.value = await api.get(String(route.params.id));
    closeAttributeDetail();
  } catch (e: any) { detailError.value = e?.message || 'No se pudo eliminar el atributo.'; }
  finally { deletingAttribute.value = false; }
}

async function openUniqueReview() {
  uniqueReviewError.value = '';
  try { pendingUniqueAbilities.value = await api.pendingUniqueAbilities(String(route.params.id)) as PendingUniqueAbility[]; showUniqueReview.value = true; }
  catch (e: any) { uniqueReviewError.value = e?.message || 'No se pudieron cargar las habilidades únicas pendientes.'; }
}
async function decideUniqueAbility(ability: PendingUniqueAbility, decision: 'accepted' | 'rejected') {
  uniqueReviewBusy.value = ability.name; uniqueReviewError.value = '';
  try {
    character.value = await api.decideUniqueAbility(String(route.params.id), ability.name, decision) as Character;
    pendingUniqueAbilities.value = pendingUniqueAbilities.value.filter(item => item.name !== ability.name);
    if (!pendingUniqueAbilities.value.length) showUniqueReview.value = false;
  } catch (e: any) { uniqueReviewError.value = e?.message || 'No se pudo guardar la decisión.'; }
  finally { uniqueReviewBusy.value = ''; }
}
async function deleteCharacter() {
  if (!confirm(`¿Borrar definitivamente a ${character.value?.name}? Esta acción no se puede deshacer.`)) return;
  try { await api.deleteCharacter(String(route.params.id)); back(); } catch (e: any) { error.value = e?.message || 'No se pudo borrar el personaje.'; }
}
function requirementLines(requirements: unknown) {
  const labels: Record<string, string> = {
    EvolutivoCurva: 'Evolución curva', EvoluccionCurva: 'Evolución curva', CruzarBifrost: 'Cruzar Bifrost', FisicaQuimica: 'Física/Química', Enganno: 'Engaño', SentirYggdrasil: 'Sentir Yggdrasil', Astronavegar: 'Astronavegar', Atractivo: 'Atractivo', Buscar: 'Buscar', Conduccion: 'Conducción', Deporte: 'Deporte', Destreza: 'Destreza', Diplomacia: 'Diplomacia', Einherjer: 'Einherjer', Esconderse: 'Esconderse', Esquiva: 'Esquiva', Fuerza: 'Fuerza', Informatica: 'Informática', Intimidar: 'Intimidar', Labia: 'Labia', Liderazgo: 'Liderazgo', Medicina: 'Medicina', Provocar: 'Provocar', Punteria: 'Puntería', Resistencia: 'Resistencia', Heroe: 'Héroe', Norna: 'Norna', Alfar: 'Alfar', Valkiria: 'Valkiria', Risa: 'Risa', Dvergr: 'Dvergr'
  };
  const ignored = new Set(['Nombre', 'Descripcion', 'Lanzamiento', 'Coste', 'Prueba', 'Unica']);
  const alternatives = Array.isArray(requirements) ? requirements : [requirements];
  return alternatives.map((alternative, index) => {
    if (!alternative || typeof alternative !== 'object') return { title: alternatives.length > 1 ? `Alternativa ${index + 1}` : 'Requisito', items: [{ label: 'Requisito', value: String(alternative) }] };
    const record = alternative as Record<string, unknown>;
    const items = Object.entries(record).filter(([key, value]) => !ignored.has(key) && typeof value === 'number').map(([key, value]) => ({ label: labels[key] || key, value: String(value) }));
    return { title: alternatives.length > 1 ? `Alternativa ${index + 1}` : 'Requisitos', items };
  }).filter(group => group.items.length);
}
function back() { campaign.value?.id ? router.push({ path: '/', query: { campaign: campaign.value.id } }) : router.push('/'); }

const obtainedAbilities = computed(() => (character.value?.abilities ?? []).map(name =>
  abilityCatalog.value.find(ability => ability.name === name) ?? { name }));

const lastUpgradeAbilities = computed(() => (lastUpgrade.value?.abilities ?? []).map(name =>
  abilityCatalog.value.find(ability => ability.name === name) ?? { name }));

function abilityTestValue(ability: Ability | null): string | undefined {
  if (!ability) return undefined;
  if (ability.test) return ability.test;
  try {
    const alternatives = JSON.parse(ability.alternativesJson || '[]');
    return Array.isArray(alternatives) ? alternatives[0]?.Prueba : undefined;
  } catch {
    return undefined;
  }
}

function abilityActivationDetails(ability: Ability | null) {
  const rawTest = abilityTestValue(ability)?.trim();
  if (!rawTest) return null;

  const match = rawTest.match(/^(.+?)\s+(\d+|\*)\+?$/);
  const testName = match?.[1]?.trim() ?? rawTest;
  const normalizedName = testName.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ');
  const key = normalizedName === 'cruzar bifrost' ? 'cruzarbifrost'
    : normalizedName === 'sentir yggdrasil' ? 'sentiryggdrasil'
      : normalizedName === 'fisica/quimica' ? 'fisicaquimica'
        : normalizedName.replace(/[ /]/g, '');
  const isKnownAttribute = majorKeys.includes(key) || minorKeys.includes(key) || Boolean(customMinor(key));
  if (!isKnownAttribute) {
    return { key, rawTest, testName, difficulty: match?.[2] === '*' ? null : match?.[2] ? Number(match[2]) : null, score: null, plusOne: null, plusD6: null };
  }

  const custom = customMinor(key);
  const score = displayedAttributeTotal(key, custom?.total ?? attributes.value[key] ?? 0);
  return {
    rawTest,
    testName,
    key,
    difficulty: match?.[2] === '*' ? null : match?.[2] ? Number(match[2]) : null,
    score,
    plusOne: custom?.plusOne ?? oneBonus(key, score, majorKeys.includes(key)),
    plusD6: custom?.plusD6 ?? d6Bonus(key, score, majorKeys.includes(key)),
  };
}

const abilityActivationTest = computed(() => {
  const details = abilityActivationDetails(selectedAbility.value);
  if (!details) return null;
  return { ...details, difficulty: details.difficulty === null ? null : `${details.difficulty}+` };
});

function canRollAbility(ability: Ability): boolean {
  const details = abilityActivationDetails(ability);
  return details?.score !== null && details?.score !== undefined && details?.difficulty !== null && details?.difficulty !== undefined;
}

async function loadAbilities() {
  abilityCatalogLoading.value = true; abilityCatalogError.value = '';
  try { abilityCatalog.value = await api.abilities() as Ability[]; }
  catch (e: any) { abilityCatalogError.value = e?.message || 'No se pudo cargar el catálogo de habilidades.'; }
  finally { abilityCatalogLoading.value = false; }
}

function openAbilityDetail(ability: Ability) { selectedAbility.value = ability; }
function closeAbilityDetail() { selectedAbility.value = null; }
function onEscape(event: KeyboardEvent) {
  if (event.key !== 'Escape') return;
  if (showAttributeRoll.value) closeAttributeRoll();
  else if (showHistory.value) closeHistory();
  else closeAbilityDetail();
}

onMounted(() => { load(); loadAbilities(); window.addEventListener('keydown', onEscape); });
onBeforeUnmount(() => { window.removeEventListener('keydown', onEscape); if (trainingPreviewTimer) clearTimeout(trainingPreviewTimer); });

watch(() => route.params.id, (id, previousId) => { if (id && id !== previousId) load(); });

watch(() => route.query.mode, (mode, previousMode) => {
  if (mode === previousMode || !character.value) return;
  if (mode === 'edit' && !editing.value) startEdit();
  else if (mode !== 'edit' && editing.value) router.replace({ query: queryWithEditMode(true) });
});

watch(() => route.name, async (name) => {
  if (name === 'character-history') {
    sheetView.value = 'sheet';
    await loadHistory();
    return;
  }

  showHistory.value = false;
  if (name === 'character-abilities') sheetView.value = 'abilities';
  else if (name === 'character-inventory') sheetView.value = 'inventory';
  else if (name === 'character-sheet') sheetView.value = 'sheet';
}, { immediate: true });

</script>



<template>

  <main class="app-shell character-sheet-shell">

    <header class="topbar"><div class="brand"><img class="brand-logo" src="/logo.png" alt="Deus ex Machina" /></div></header>

    <p v-if="error" class="error-banner" role="alert">{{ error }} <button class="button button-quiet" @click="load">Reintentar</button></p>

    <div v-if="loading" class="sheet-state">Cargando ficha…</div>

    <div v-else-if="character" class="sheet-layout">

      <aside class="sheet-sidebar">

        <div class="sheet-portrait"><img v-if="character.imageUrl" :src="character.imageUrl" :alt="`Retrato de ${character.name}`" /><span v-else>{{ initials(character.name) }}</span></div>

        <p class="eyebrow accent">PERSONAJE</p><h1>{{ character.name }}</h1>

        <dl class="sheet-meta"><dt>Campaña</dt><dd>{{ campaign?.name || 'Sin campaña' }}</dd><dt>Versión</dt><dd>{{ character.closed ? 'Cerrada' : 'Borrador' }}</dd><dt>Último guardado</dt><dd>{{ savedAt }}</dd></dl>
        <button v-if="props.isDirector" class="button button-quiet" type="button" @click="openEditorsModal">Emails con edición</button>
        <p v-if="closeError" class="error-banner" role="alert">{{ closeError }}</p>

         <template v-if="editing"><button class="button button-primary" :disabled="closeBusy || levelBusy || modifierSaveBusy" @click="closeDraft">{{ closeBusy ? 'Guardando…' : 'Guardar' }}</button><button class="button button-quiet" type="button" :disabled="cancelChangesBusy" @click="cancelChanges">{{ cancelChangesBusy ? 'Cancelando…' : 'Cancelar cambios' }}</button><button class="button button-quiet" type="button" @click="showLegacyImport=true; legacyError=''">Cargar legacy</button><button class="button button-quiet" type="button" @click="openCurrentUpgrade">Ver subida actual</button></template><template v-else><button v-if="canEdit" class="button button-quiet" type="button" @click="startEdit">Editar ficha</button><button class="button button-quiet" type="button" @click="exportLegacy">Exportar legacy</button><button class="button button-quiet" type="button" @click="openLastUpgrade">Última subida</button></template><button class="button button-quiet" type="button" @click="openHistory">Historial</button><button class="button button-quiet" type="button" @click="openInventory">Inventario</button><button v-if="props.isDirector && (character.pendingUniqueAbilities?.length ?? 0)" class="button button-primary unique-review-trigger" type="button" @click="openUniqueReview">Revisar habilidades únicas</button><button class="button button-quiet" type="button" @click="navigateToSection(sheetView === 'sheet' ? 'abilities' : 'sheet')">{{ sheetView === 'sheet' ? 'Ver habilidades' : 'Ver ficha' }}</button><button class="button button-quiet" type="button" @click="openProfileModal">{{ editing ? 'Editar perfil' : 'Ver perfil' }}</button><button class="button button-quiet" type="button" @click="back">Volver</button><button v-if="props.isDirector" class="button button-danger" type="button" @click="deleteCharacter">Borrar personaje</button>
        <p v-if="editing" class="field-hint">Haz clic en un atributo para editar sus modificadores varios.</p><p v-if="modifierError" class="error-banner" role="alert">{{ modifierError }}</p>

      </aside>


      <div v-if="showEditorsModal" class="modal-backdrop" @click.self="showEditorsModal=false">
        <section class="modal-card creation-modal" role="dialog" aria-modal="true" aria-labelledby="editors-modal-title">
          <header class="modal-header"><div><p class="eyebrow accent">ACCESO DEL PERSONAJE</p><h2 id="editors-modal-title">Emails con edición</h2><p class="modal-copy">Solo los miembros de la campaña añadidos aquí podrán modificar esta ficha.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="showEditorsModal=false">×</button></header>
          <div class="modal-body">
            <p v-if="editorError" class="error-banner" role="alert">{{ editorError }}</p>
            <ul v-if="campaignEditorCandidates.length" class="campaign-members"><li v-for="email in campaignEditorCandidates" :key="email"><span>{{ email }}</span><button class="button" :class="hasEditorAccess(email) ? 'button-quiet' : 'button-primary'" type="button" :disabled="editorBusy" @click="toggleEditor(email)">{{ hasEditorAccess(email) ? 'Quitar' : 'Añadir' }}</button></li></ul>
            <p v-else class="field-hint">No hay miembros activos en la campaña a los que asignar edición.</p>
          </div>
        </section>
      </div>

      <div v-if="showProfileModal" class="modal-backdrop" @click.self="showProfileModal=false">
        <section class="modal-card profile-modal" role="dialog" aria-modal="true" aria-labelledby="profile-modal-title">
          <header class="modal-header"><div><p class="eyebrow accent">PERSONAJE · CONFIGURACIÓN</p><h2 id="profile-modal-title">Perfil Einherjer</h2><p class="modal-copy">{{ editing ? 'Actualiza la edad y los datos de origen del personaje.' : 'Consulta la edad y los datos de origen del personaje.' }}</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="showProfileModal=false">×</button></header>
          <div v-if="!editing" class="modal-body profile-summary profile-summary-modal"><span>Edad actual<strong>{{ character.sheetAge ?? 'No indicada' }}</strong></span><span>Origen<strong>{{ originLabel(character.einherjerOrigin) }}</strong></span><span>Despertado<strong>{{ character.awakened ? 'Sí' : 'No' }}</strong></span><span v-if="character.awakened && character.awakeningAge !== null">Edad de despertar<strong>{{ character.awakeningAge }}</strong></span></div>
          <div v-else class="modal-body profile-modal-form">
            <section class="modal-section-panel"><h3 class="modal-section-title">Datos del personaje</h3><div class="profile-form-grid"><label class="modal-field"><span>Edad inicial</span><input v-model.number="character.startingAge" type="number" min="0" :max="character.sheetAge ?? undefined" required /></label><label class="modal-field"><span>Edad actual</span><input v-model.number="character.sheetAge" type="number" :min="(character.startingAge ?? 0) + 1" required /></label><label class="modal-field"><span>Origen</span><select v-model="character.einherjerOrigin" required><option :value="null" disabled>Selecciona un origen</option><option value="converted">Convertido</option><option value="born_human">Nacido de humanos</option><option value="born_einherjer">Nacido de Einherjer</option></select></label></div></section>
            <section class="modal-section-panel"><h3 class="modal-section-title">Despertar</h3><div class="profile-form-grid"><div class="profile-awakening-field"><span class="field-label">¿Ha despertado?</span><div class="guided-answer-grid"><button class="answer-card" type="button" :class="{ selected: character.awakened === false }" @click="setAwakened(false)">No</button><button class="answer-card" type="button" :class="{ selected: character.awakened === true }" @click="setAwakened(true)">Sí</button></div></div><label v-if="character.awakened" class="modal-field"><span>Edad de despertar / conversión</span><input v-model.number="character.awakeningAge" type="number" :min="character.startingAge ?? 0" :max="character.sheetAge ?? undefined" required /></label></div></section>
          </div>
          <footer class="modal-actions"><button class="button button-primary" type="button" @click="editing ? closeDraft() : showProfileModal=false" :disabled="editing && closeBusy">{{ editing ? (closeBusy ? 'Guardando…' : 'Guardar perfil') : 'Cerrar' }}</button></footer>
        </section>
      </div>
      <div v-if="showMinorModal" class="modal-backdrop" @click.self="showMinorModal=false">
        <div class="modal-card minor-modal" role="dialog" aria-modal="true" aria-labelledby="minor-modal-title">
          <header class="modal-header">
            <div>
              <p class="eyebrow accent">PERSONAJE · CONFIGURACIÓN</p>
              <h2 id="minor-modal-title">Añadir atributo menor</h2>
              <p class="modal-copy">Amplía esta ficha con un atributo propio de la campaña.</p>
            </div>
            <button type="button" class="modal-close" aria-label="Cerrar ventana" @click="showMinorModal=false">×</button>
          </header>

          <div class="modal-body">
            <div class="modal-section">
              <label class="modal-field"><span>Tipo de atributo</span><select v-model="minorKind"><option value="GALDR">Galdr</option><option value="ASTRONAVEGAR">Astronavegar</option><option value="FORJA">Forja</option><option value="CUSTOM">Personalizado</option></select></label>
            </div>

            <template v-if="minorKind==='CUSTOM'">
              <div class="modal-section modal-section-panel">
                <p class="modal-section-title">Definición personalizada</p>
                <label class="modal-field"><span>Nombre</span><input v-model="customName" placeholder="Ej. Navegación astral" autocomplete="off"></label>
                <label class="modal-field"><span>Fórmula del máximo</span><input v-model="customFormula" placeholder="(fisico + estudio) / 2" autocomplete="off" :aria-invalid="customFormula.length > 0 && !formulaIsValid"></label>
                <div class="formula-builder" aria-label="Constructor de fórmula">
                  <div class="formula-preview"><span>{{ customFormula || 'Construye la fórmula con los botones' }}</span><span v-if="customFormula && !formulaIsValid" class="formula-invalid">Fórmula no válida</span></div>
                  <div class="formula-buttons"><button type="button" v-for="t in formulaTokens" :key="t" class="formula-token" @click="insertToken(t)">{{t}}</button><button type="button" class="formula-action" @click="customFormula=customFormula.slice(0,-1)">Borrar</button><button type="button" class="formula-action" @click="clearFormula">Limpiar</button></div>
                  <p class="field-hint">Usa atributos, números, operadores y las funciones <code>min</code> y <code>max</code>.</p>
                </div>
                <label class="modal-field"><span>Bonificaciones de</span><select v-model="customSource"><option v-for="source in bonusSourceOptions" :key="source.key" :value="source.key">{{ source.label }}</option></select></label>
              </div>
            </template>

            <p v-if="minorError" class="error-banner">{{ minorError }}</p>
          </div>

          <footer class="modal-actions">
            <button class="button button-quiet" @click="showMinorModal=false">Cancelar</button>
            <button class="button button-primary" :disabled="minorBusy || (minorKind==='CUSTOM' && (!customName.trim() || !formulaIsValid || !customSource))" @click="addMinor">{{ minorBusy ? 'Añadiendo…' : 'Añadir atributo' }}</button>
          </footer>
        </div>
      </div>

      <div v-if="showLegacyImport" class="modal-backdrop" @click.self="showLegacyImport=false">
        <section class="modal-card legacy-modal" role="dialog" aria-modal="true" aria-labelledby="legacy-import-title">
          <header class="modal-header"><div><p class="eyebrow accent">BACKUP LEGACY</p><h2 id="legacy-import-title">Cargar legacy</h2><p class="modal-copy">Sustituirá todas las puntuaciones y modificadores del borrador. Los cambios quedan pendientes de guardar.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="showLegacyImport=false">×</button></header>
          <div class="modal-body"><label class="modal-field"><span>Código legacy</span><textarea v-model="legacyCode" class="legacy-textarea" rows="12" autofocus></textarea></label><p v-if="legacyError" class="error-banner" role="alert">{{ legacyError }}</p></div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="showLegacyImport=false">Cancelar</button><button class="button button-primary" type="button" :disabled="legacyBusy || !legacyCode.trim()" @click="importLegacy">{{ legacyBusy ? 'Cargando…' : 'Cargar legacy' }}</button></footer>
        </section>
      </div>

      <div v-if="showLegacyExport" class="modal-backdrop" @click.self="showLegacyExport=false">
        <section class="modal-card legacy-modal" role="dialog" aria-modal="true" aria-labelledby="legacy-export-title">
          <header class="modal-header"><div><p class="eyebrow accent">BACKUP LEGACY</p><h2 id="legacy-export-title">Exportar legacy</h2><p class="modal-copy">Copia este código y pégalo en «Cargar Backup» del HTML estático.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="showLegacyExport=false">×</button></header>
          <div class="modal-body"><textarea readonly class="legacy-textarea" rows="12" :value="legacyExportCode"></textarea></div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="showLegacyExport=false">Cerrar</button></footer>
        </section>
      </div>

      <div v-if="showAllocationModal && allocationDraft" class="modal-backdrop" @click.self="closeAllocation">
        <section ref="allocationModal" class="modal-card allocation-modal" role="dialog" aria-modal="true" aria-labelledby="allocation-modal-title">
          <header class="modal-header"><div><p class="eyebrow accent">PERSONAJE · PROGRESIÓN</p><h2 id="allocation-modal-title">{{ allocationMode === 'all' ? `Asignación · nivel ${allocationDraft.level} (${allocationStep}/${allocationTotal})` : `Asignación · nivel ${allocationDraft.level}` }}</h2><p class="modal-copy">Los cambios sólo se guardan al confirmar este paso.</p></div><button type="button" class="modal-close" aria-label="Cerrar ventana" @click="closeAllocation">×</button></header>
          <div class="modal-body allocation-body">
            <div class="allocation-counters"><div><span>Evolución restante</span><strong>{{ allocationBudget.evolutionRemaining }}</strong><small>{{ allocationBudget.evolutionSpent }} / {{ allocationBudget.evolutionAvailable }} gastados</small></div><div><span>Genética restante</span><strong>{{ allocationBudget.geneticsRemaining }}</strong><small>{{ allocationBudget.geneticsSpent }} / {{ allocationBudget.geneticsAvailable }} gastados</small></div></div>
             <section class="allocation-section"><h3>Atributos mayores</h3><div class="allocation-grid"><div v-for="[key, value] in allocationMajorAttributes" :key="key" class="allocation-field"><span>{{ attributeLabels[key] || key }}</span><div class="allocation-edit-row"><input class="allocation-rank-input" type="number" :min="allocationDraft.baseAttributes[key] ?? 0" :max="allocationAffordableMax(key, 10, allocationDraft.baseAttributes)" :disabled="allocationMajorAtLimit(key)" step="1" :value="value" @input="updateAllocationAttribute(key, $event)"><div class="allocation-preview"><span>Mod. {{ allocationModifierTotal(key) >= 0 ? '+' : '' }}{{ allocationModifierTotal(key) }}</span><span>Puntuación <strong>{{ allocationScore(key) }}</strong></span><span :class="{ 'bonus-changed': allocationBonusChanged(key, 'plusOne') }">+1 <strong>{{ allocationBonuses(key).plusOne }}</strong></span><span :class="{ 'bonus-changed': allocationBonusChanged(key, 'plusD6') }">+D6 <strong>{{ allocationBonuses(key).plusD6 }}</strong></span></div></div></div></div></section>
             <section class="allocation-section"><h3>Atributos menores</h3><div class="allocation-grid"><div v-for="[key, value] in allocationMinorAttributes" :key="key" class="allocation-field"><span>{{ attributeLabels[key] || customAllocationMinor(key)?.name || key }} <small v-if="allocationMinorCap(key) !== null" :class="{ 'allocation-cap-reached': value >= (allocationMinorCap(key) ?? Infinity) }">máx. {{ allocationMinorCap(key) }} · {{ allocationCapFormula(key) }}</small></span><div class="allocation-edit-row"><input class="allocation-rank-input" type="number" :min="customAllocationMinor(key) ? (allocationDraft.baseMinorAttributes[key] ?? 0) : (allocationDraft.baseAttributes[key] ?? 0)" :max="customAllocationMinor(key) ? allocationEvolutionMax(key, true) : allocationEvolutionMax(key, false)" :disabled="customAllocationMinor(key) ? allocationEvolutionAtLimit(key, true) : allocationEvolutionAtLimit(key, false)" step="1" :value="value" @input="customAllocationMinor(key) ? updateAllocationMinor(key, $event) : updateAllocationAttribute(key, $event)"><div class="allocation-preview"><span>Mod. {{ allocationModifierTotal(key) >= 0 ? '+' : '' }}{{ allocationModifierTotal(key) }}</span><span>Puntuación <strong>{{ allocationScore(key) }}</strong></span><span :class="{ 'bonus-changed': allocationBonusChanged(key, 'plusOne') }">+1 <strong>{{ allocationBonuses(key).plusOne }}</strong></span><span :class="{ 'bonus-changed': allocationBonusChanged(key, 'plusD6') }">+D6 <strong>{{ allocationBonuses(key).plusD6 }}</strong></span></div></div></div></div></section>
             <section class="allocation-section"><h3>Genética</h3><div class="allocation-grid allocation-genetics-grid"><label v-for="[key, value] in allocationGenetics" :key="key" class="allocation-field"><span>{{ geneticLabels[key] || key }}</span><input class="allocation-rank-input" type="number" :min="allocationDraft.baseGenetics[key] ?? 0" :max="allocationGeneticMax(key)" :disabled="allocationGeneticAtLimit(key) && value <= (allocationDraft.baseGenetics[key] ?? 0)" step="1" :value="value" @input="updateAllocationGenetics(key, $event)"></label></div></section>
            <p v-if="!allocationValid" class="error-banner" role="alert">
              <template v-if="allocationBudget.geneticsRemaining > 0">Debes asignar los {{ allocationBudget.geneticsRemaining }} puntos de genética restantes antes de aceptar la subida.</template>
              <template v-else>Has superado el presupuesto de evolución disponible. Reduce algún rango antes de guardar.</template>
            </p>
          </div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeAllocation">Cancelar</button><button class="button button-primary" type="button" :disabled="levelBusy || !allocationValid" @click="submitAllocation">{{ levelBusy ? 'Guardando…' : (allocationMode === 'all' && allocationStep < allocationTotal ? 'Siguiente nivel' : 'Guardar') }}</button></footer>
        </section>
      </div>

        <div v-if="showExperienceModal" class="modal-backdrop" @click.self="showExperienceModal=false">
        <div class="modal-card experience-modal" role="dialog" aria-modal="true" aria-labelledby="experience-modal-title">
          <header class="modal-header"><div><p class="eyebrow accent">PERSONAJE · PROGRESIÓN</p><h2 id="experience-modal-title">Añadir experiencia</h2><p class="modal-copy">La experiencia se suma a la cantidad actual del personaje.</p></div><button type="button" class="modal-close" aria-label="Cerrar ventana" @click="showExperienceModal=false">×</button></header>
          <div class="modal-body"><label class="modal-field"><span>Experiencia a añadir</span><input v-model.number="experienceAmount" type="number" min="1" step="1" autofocus @keyup.enter="addExperience"></label><p v-if="experienceError" class="error-banner" role="alert">{{ experienceError }}</p></div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="showExperienceModal=false">Cancelar</button><button class="button button-primary" type="button" :disabled="experienceBusy" @click="addExperience">{{ experienceBusy ? 'Añadiendo…' : 'Añadir experiencia' }}</button></footer>
        </div>
      </div>

      <div v-if="showAttributeDetail" class="modal-backdrop" @click.self="closeAttributeDetail">
        <section class="modal-card attribute-detail-modal" role="dialog" aria-modal="true" aria-labelledby="attribute-detail-title">
          <header class="modal-header"><div><p class="eyebrow accent">DETALLE DEL ATRIBUTO</p><h2 id="attribute-detail-title">{{ attributeDetail?.name || 'Atributo' }}</h2></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeAttributeDetail">×</button></header>
          <div class="modal-body" v-if="detailLoading"><p class="sheet-state">Cargando atributo…</p></div>
          <div class="modal-body" v-else-if="attributeDetail">
            <p v-if="detailError" class="error-banner" role="alert">{{ detailError }}</p>
            <div class="attribute-detail-summary"><div><span>{{ attributeDetail.type === 'DERIVED' ? 'Valor total' : 'Puntuación total actual' }}</span><strong>{{ detailTotal() }}</strong></div><div v-if="attributeDetail.type !== 'DERIVED'"><span>Bonificadores +1</span><strong>+{{ editing ? detailBonus('plusOne') : attributeDetail.plusOne }}</strong></div><div v-if="attributeDetail.type !== 'DERIVED'"><span>Bonificadores +D6</span><strong>+{{ editing ? detailBonus('plusD6') : attributeDetail.plusD6 }}D6</strong></div></div>
            <div class="detail-grid"><template v-if="attributeDetail.type !== 'DERIVED'"><p><span>Rangos actuales</span><strong>{{ attributeDetail.ranks }}</strong></p><p><span>Rangos máximos</span><strong>{{ attributeDetail.maxRanks ?? 'No aplica' }}</strong></p></template><p class="detail-wide"><span>{{ attributeDetail.type === 'DERIVED' ? 'Fórmula' : 'Fórmula máxima' }}</span><strong>{{ attributeDetail.formula }}<template v-if="attributeDetail.type === 'DERIVED'"> = {{ attributeDetail.calculatedValue }}</template><template v-else-if="attributeDetail.maxRanks !== null"> = {{ attributeDetail.calculatedValue }}</template></strong></p></div>
            <section class="detail-section"><h3>Modificadores varios</h3><ul v-if="attributeDetail.modifiers.length" class="modifier-list"><li v-for="modifier in attributeDetail.modifiers" :key="modifier.name"><span>{{ modifierDisplayName(modifier.name) }}</span><strong>{{ modifier.value > 0 ? '+' : '' }}{{ modifier.value }}</strong></li></ul><p v-else class="sheet-muted">Sin modificadores varios.</p></section><section v-if="editing" class="detail-section modifier-editor"><h3>Editar modificadores</h3><div v-for="(modifier, index) in modifierRows(attributeDetail.key)" :key="index" class="modifier-edit-row"><input v-model="modifier.name" aria-label="Nombre del modificador" placeholder="Nombre"><input v-model.number="modifier.value" type="number" step="1" aria-label="Valor del modificador"><button class="button button-danger" type="button" @click="removeModifier(attributeDetail.key, index)">Eliminar</button></div><button class="button button-quiet" type="button" @click="addModifier(attributeDetail.key)">Añadir modificador</button></section>
            <section v-if="attributeDetail.type !== 'DERIVED'" class="detail-section"><h3>Progresión</h3><div class="progression-groups"><div><h4>+1</h4><div class="progression-list"><span v-for="item in attributeDetail.progressions.filter(p => p.kind === '+1')" :key="`${item.kind}-${item.number}`" :class="['progression-item', { obtained: editing ? detailTotal() >= item.threshold : item.obtained }]">{{ item.threshold }}</span></div></div><div><h4>+D6</h4><div class="progression-list"><span v-for="item in attributeDetail.progressions.filter(p => p.kind === '+D6')" :key="`${item.kind}-${item.number}`" :class="['progression-item', { obtained: editing ? detailTotal() >= item.threshold : item.obtained }]">{{ item.threshold }}</span></div></div></div></section>
          </div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeAttributeDetail">Cerrar</button><button v-if="attributeDetail?.deletable" class="button button-danger" type="button" :disabled="deletingAttribute" @click="deleteAttribute">{{ deletingAttribute ? 'Eliminando…' : 'Eliminar' }}</button></footer>
        </section>
      </div>

      <div v-if="showUniqueReview" class="modal-backdrop" @click.self="showUniqueReview=false">
        <section class="modal-card unique-review-modal" role="dialog" aria-modal="true" aria-labelledby="unique-review-title">
          <header class="modal-header"><div><p class="eyebrow accent">DIRECTOR</p><h2 id="unique-review-title">Habilidades únicas pendientes</h2><p class="modal-copy">Acepta para incorporarla a la ficha o rechaza para ocultarla definitivamente al jugador.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="showUniqueReview=false">×</button></header>
          <div class="modal-body"><p v-if="uniqueReviewError" class="error-banner" role="alert">{{ uniqueReviewError }}</p><p v-if="!pendingUniqueAbilities.length" class="sheet-state">No hay habilidades únicas pendientes.</p><article v-for="ability in pendingUniqueAbilities" :key="ability.name" class="unique-ability-review"><div class="unique-ability-heading"><span class="unique-badge">ÚNICA</span><h3>{{ ability.name }}</h3></div><p class="unique-ability-description">{{ ability.description || 'Sin descripción disponible.' }}</p><dl class="ability-meta"><div><dt>Lanzamiento</dt><dd>{{ ability.launchType || '—' }}</dd></div><div><dt>Coste</dt><dd>{{ ability.cost ?? '—' }}</dd></div><div><dt>Prueba</dt><dd>{{ ability.test || '—' }}</dd></div></dl><div class="unique-requirements"><h4>Requisitos</h4><div v-for="group in requirementLines(ability.requirements)" :key="group.title" class="requirement-group"><span class="requirement-group-title">{{ group.title }}</span><div class="requirement-chips"><span v-for="item in group.items" :key="item.label" class="requirement-chip"><strong>{{ item.label }}</strong><em>{{ item.value }}</em></span></div></div></div><div class="modal-actions"><button class="button button-danger" type="button" :disabled="uniqueReviewBusy === ability.name" @click="decideUniqueAbility(ability, 'rejected')">Rechazar</button><button class="button button-primary" type="button" :disabled="uniqueReviewBusy === ability.name" @click="decideUniqueAbility(ability, 'accepted')">{{ uniqueReviewBusy === ability.name ? 'Guardando…' : 'Aceptar' }}</button></div></article></div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="showUniqueReview=false">Cerrar</button></footer>
        </section>
      </div>

       <div v-if="selectedAbility" class="modal-backdrop ability-detail-backdrop" @click.self="closeAbilityDetail">
        <section class="modal-card ability-detail-modal" role="dialog" aria-modal="true" aria-labelledby="ability-detail-title" tabindex="-1">
          <header class="modal-header"><div><p class="eyebrow accent">HABILIDAD OBTENIDA</p><div class="ability-title-row"><h2 id="ability-detail-title">{{ selectedAbility.name }}</h2><button v-if="canRollAbility(selectedAbility)" class="ability-roll-trigger" type="button" :aria-label="`Tirar D10 para ${selectedAbility.name}`" @click="openAbilityRoll(selectedAbility)"><img src="/diceD10.png" alt="" aria-hidden="true" /></button></div></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeAbilityDetail">×</button></header>
          <div class="modal-body ability-detail-body">
            <p class="ability-description">{{ selectedAbility.description || 'Sin descripción disponible.' }}</p>
            <dl class="ability-meta"><div><dt>Lanzamiento</dt><dd>{{ selectedAbility.launchType || '—' }}</dd></div><div><dt>Coste</dt><dd>{{ selectedAbility.cost ?? '—' }}</dd></div><div><dt>Prueba de activación</dt><dd v-if="abilityActivationTest">{{ abilityActivationTest.difficulty ? abilityActivationTest.testName + ' ' + abilityActivationTest.difficulty : abilityActivationTest.rawTest }}<template v-if="abilityActivationTest.score !== null"> (+{{ abilityActivationTest.plusOne }} +{{ abilityActivationTest.plusD6 }}D6)</template></dd><dd v-else>—</dd></div></dl>
          </div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeAbilityDetail">Cerrar</button></footer>
        </section>
      </div>

<div v-if="showHistory" class="modal-backdrop" @click.self="closeHistory"><section class="modal-card history-modal" role="dialog" aria-modal="true" aria-labelledby="history-title"><header class="modal-header"><div><p class="eyebrow accent">FICHA · HISTORIAL</p><h2 id="history-title">Versiones cerradas</h2><p class="modal-copy">Recorre las versiones guardadas y recupera una como nueva versión actual.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeHistory">×</button></header><div class="modal-body"><p v-if="historyLoading" class="sheet-state">Cargando historial…</p><p v-else-if="historyError" class="error-banner" role="alert">{{ historyError }}</p><p v-else-if="!history.length" class="sheet-state">Aún no hay versiones cerradas.</p><ol v-else class="history-list"><li v-for="version in history" :key="version.id" class="history-item"><button class="history-select" type="button" @click="selectedHistory=version"><div><strong>Nivel {{ version.level }}</strong><span>{{ new Date(version.createdAt).toLocaleString('es-ES') }}</span><small>{{ version.experience }} PX · {{ Object.keys(version.snapshot?.attributes || {}).length }} atributos</small></div></button><button class="button button-primary" type="button" :disabled="historyRecovering === version.id" @click.stop="recoverHistory(version)">{{ historyRecovering === version.id ? 'Recuperando…' : 'Recuperar' }}</button></li></ol><section v-if="selectedHistory" class="history-detail"><h3>Versión seleccionada</h3><p><strong>{{ selectedHistory.snapshot?.name || character.name }}</strong> · Nivel {{ selectedHistory.level }} · {{ selectedHistory.experience }} PX</p><p class="sheet-muted">Evolución: {{ selectedHistory.snapshot?.evolutionPoints ?? 'no disponible' }} · Genética: {{ selectedHistory.snapshot?.geneticsPoints ?? 'no disponible' }}</p><p class="sheet-muted">Habilidades: {{ (selectedHistory.snapshot?.abilities || []).length }} · Modificadores: {{ Object.values(selectedHistory.snapshot?.modifiers || {}).flat().length }}</p></section></div><footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeHistory">Cerrar</button></footer></section></div>

      <div v-if="showLastUpgrade" class="modal-backdrop" @click.self="closeLastUpgrade"><section class="modal-card last-upgrade-modal" role="dialog" aria-modal="true" aria-labelledby="last-upgrade-title"><header class="modal-header"><div><p class="eyebrow accent">PROGRESIÓN</p><h2 id="last-upgrade-title">{{ currentUpgradeMode ? 'Ver subida actual' : 'Última subida' }}</h2><p v-if="lastUpgrade?.available" class="modal-copy">Comparación entre la versión actual y el nivel {{ lastUpgrade.previous?.level }}.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeLastUpgrade">×</button></header><div class="modal-body"><p v-if="lastUpgradeLoading" class="sheet-state">{{ currentUpgradeMode ? 'Comparando subida actual…' : 'Comparando versiones cerradas…' }}</p><p v-else-if="lastUpgradeError" class="error-banner" role="alert">{{ lastUpgradeError }}</p><p v-else-if="!lastUpgrade?.available" class="sheet-state">Aún no existe una versión cerrada anterior para comparar.</p><template v-else><section class="upgrade-section"><h3>Nuevas puntuaciones</h3><ul v-if="lastUpgrade.scores?.length" class="upgrade-list"><li v-for="change in lastUpgrade.scores" :key="`${change.type}-${change.key}`"><span>{{ upgradeScoreLabel(change) }}</span><strong>{{ change.before }} → {{ change.after }} <em>+{{ change.increase }}</em></strong></li></ul><p v-else class="sheet-muted">Sin nuevas puntuaciones.</p></section><section class="upgrade-section"><h3>Modificadores varios</h3><ul v-if="lastUpgrade.modifiers?.length" class="upgrade-list"><li v-for="change in lastUpgrade.modifiers" :key="`${change.key}-${change.name}`"><span>{{ attributeLabels[change.key] || change.key }} · {{ change.name }}</span><strong>{{ change.before ?? '—' }} → {{ change.after ?? '—' }}</strong></li></ul><p v-else class="sheet-muted">Sin cambios en modificadores varios.</p></section><section class="upgrade-section"><h3>Nuevos bonificadores</h3><ul v-if="lastUpgrade.bonuses?.length" class="upgrade-list"><li v-for="bonus in lastUpgrade.bonuses" :key="bonus.key"><span>{{ attributeLabels[bonus.key] || bonus.key }}</span><strong><template v-if="bonus.plusOne">+{{ bonus.plusOne }}</template><template v-if="bonus.plusOne && bonus.plusD6"> · </template><template v-if="bonus.plusD6">+{{ bonus.plusD6 }}D6</template></strong></li></ul><p v-else class="sheet-muted">Sin nuevos +1 ni +D6.</p></section><section class="upgrade-section"><h3>Nuevas habilidades</h3><ul v-if="lastUpgradeAbilities.length" class="upgrade-list"><li v-for="ability in lastUpgradeAbilities" :key="ability.name"><button class="upgrade-ability-link" type="button" @click="openAbilityDetail(ability)">{{ ability.name }}</button></li></ul><p v-else class="sheet-muted">No se han obtenido habilidades nuevas.</p></section></template></div><footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeLastUpgrade">Cerrar</button></footer></section></div>

      <div v-if="showTrainingModal" class="modal-backdrop" @click.self="showTrainingModal=false"><section class="modal-card training-modal" role="dialog" aria-modal="true" aria-labelledby="training-title"><header class="modal-header"><div><p class="eyebrow accent">TRAYECTORIA VITAL</p><h2 id="training-title">Formación</h2><p class="modal-copy">Consulta y ajusta las experiencias que desarrollaron tus atributos menores.</p></div><button class="modal-close" type="button" aria-label="Cerrar" @click="showTrainingModal=false">×</button></header><div class="modal-body"><p v-if="trainingError" class="error-banner" role="alert">{{ trainingError }}</p><div class="training-axis"><span>{{ trainingData.startingAge }} años</span><span>{{ trainingData.sheetAge }} años</span></div><section class="training-section"><h3 class="training-section-title">Formación, profesiones y ocupaciones</h3><div class="training-track"><template v-for="activity in trainingCoreActivities" :key="activity.id"><div class="training-bar" :class="activity.type.toLowerCase()" :style="trainingBarStyle(activity)" :title="`${trainingTypeLabel(activity.type)}: ${activity.name}`"></div></template></div><p v-if="!trainingCoreActivities.length" class="sheet-muted">Aún no hay formación, profesiones u ocupaciones registradas.</p><article v-for="activity in trainingCoreActivities" :key="`row-${activity.id}`" class="training-row"><div><strong>{{ activity.name }}</strong><span>{{ trainingTypeLabel(activity.type) }}<template v-if="activity.type !== 'COURSE'"> · {{ activity.startAge }}–{{ activity.endAge }} años</template><template v-if="activity.concurrent"> · compaginada</template></span><div v-if="activity.modifiers?.length" class="training-row-modifiers"><span v-for="modifier in activity.modifiers" :key="`${activity.id}-${modifier.attributeKey}`">{{ attributeLabels[modifier.attributeKey] || modifier.attributeKey }} +{{ modifier.value }}</span></div></div><div class="training-row-actions"><div v-if="editing && trainingGroup(activity).length > 1" class="training-order-actions"><button class="button button-quiet" type="button" :disabled="!canMoveTraining(activity,-1) || trainingReordering !== null" @click="moveTraining(activity,-1)" :aria-label="`Subir ${activity.name}`">↑</button><button class="button button-quiet" type="button" :disabled="!canMoveTraining(activity,1) || trainingReordering !== null" @click="moveTraining(activity,1)" :aria-label="`Bajar ${activity.name}`">↓</button></div><div class="modal-actions"><button v-if="editing" class="button button-quiet" type="button" @click="editTraining(activity)">Editar</button><button v-if="editing" class="button button-danger" type="button" @click="removeTraining(activity)">Eliminar</button></div></div></article></section><section class="training-section training-courses-section"><h3 class="training-section-title">Cursos</h3><div class="training-course-meter" :aria-label="`Cursos utilizados: ${trainingCourseUsed} de ${trainingCourseSlots}`"><div class="training-course-meter-label"><span>Cursos utilizados</span><strong>{{ trainingCourseUsed }} / {{ trainingCourseSlots }}</strong></div><div class="training-course-meter-track"><div class="training-course-meter-fill" :style="{ width: `${trainingCourseSlots ? Math.min(100, (trainingCourseUsed / trainingCourseSlots) * 100) : 0}%` }"></div></div></div><p v-if="!trainingCourseActivities.length" class="sheet-muted">Aún no hay cursos registrados.</p><article v-for="activity in trainingCourseActivities" :key="`row-${activity.id}`" class="training-row"><div><strong>{{ activity.name }}</strong><span>{{ trainingTypeLabel(activity.type) }}<template v-if="activity.type !== 'COURSE'"> · {{ activity.startAge }}–{{ activity.endAge }} años</template><template v-if="activity.concurrent"> · compaginada</template></span><div v-if="activity.modifiers?.length" class="training-row-modifiers"><span v-for="modifier in activity.modifiers" :key="`${activity.id}-${modifier.attributeKey}`">{{ attributeLabels[modifier.attributeKey] || modifier.attributeKey }} +{{ modifier.value }}</span></div></div><div class="training-row-actions"><div v-if="editing && trainingGroup(activity).length > 1" class="training-order-actions"><button class="button button-quiet" type="button" :disabled="!canMoveTraining(activity,-1) || trainingReordering !== null" @click="moveTraining(activity,-1)" :aria-label="`Subir ${activity.name}`">↑</button><button class="button button-quiet" type="button" :disabled="!canMoveTraining(activity,1) || trainingReordering !== null" @click="moveTraining(activity,1)" :aria-label="`Bajar ${activity.name}`">↓</button></div><div class="modal-actions"><button v-if="editing" class="button button-quiet" type="button" @click="editTraining(activity)">Editar</button><button v-if="editing" class="button button-danger" type="button" @click="removeTraining(activity)">Eliminar</button></div></div></article></section><section class="training-section training-total-section"><h3 class="training-section-title">Total modificadores</h3><div v-if="trainingTotalByAttribute.length" class="training-total-cards"><article v-for="group in trainingTotalByAttribute" :key="group.key" class="training-total-card"><div class="training-total-card-heading"><span>{{ attributeLabels[group.key] || group.key }}</span><strong>+{{ group.total }}</strong></div><div class="training-total-card-tags"><span v-for="(modifier,index) in group.modifiers" :key="`${group.key}-${modifier.activityName}-${index}`">{{ modifier.activityName }} +{{ modifier.value }}</span></div></article></div><p v-else class="sheet-muted">Aún no hay modificadores de trayectoria.</p></section><button v-if="editing && !showTrainingForm" class="button button-primary training-add-button" type="button" @click="openTraining">Añadir actividad</button><form v-if="showTrainingForm" class="training-form" @submit.prevent="saveTraining"><h3>{{ trainingEditingId ? 'Editar actividad' : 'Añadir actividad' }}</h3><section class="modal-section-panel"><h4 class="modal-section-title">Datos de la actividad</h4><div class="training-form-grid"><label class="modal-field">Tipo<select v-model="trainingDraft.type"><option value="FORMATION">Formación</option><option value="PROFESSION">Profesión</option><option value="OCCUPATION">Ocupación</option><option value="COURSE">Curso</option></select></label><label class="modal-field">Nombre<input v-model="trainingDraft.name" required maxlength="160"></label></div></section><section v-if="trainingDraft.type !== 'COURSE'" class="modal-section-panel"><h4 class="modal-section-title">Periodo vital</h4><div class="training-form-grid"><label class="modal-field">Edad desde<select v-model.number="trainingDraft.startAge" required><option v-for="age in trainingStartAgeOptions" :key="`start-${age}`" :value="age">{{ age }} años</option></select></label><label class="modal-field">Edad hasta<select v-model.number="trainingDraft.endAge" required><option v-for="age in trainingEndAgeOptions" :key="`end-${age}`" :value="age">{{ age }} años</option></select></label></div><p class="field-hint">Duración: {{ Math.max(0, Number(trainingDraft.endAge || 0) - Number(trainingDraft.startAge || 0)) }} años</p><label v-if="trainingDraft.type === 'OCCUPATION'" class="training-option"><input v-model="trainingDraft.concurrent" type="checkbox"> <span><strong>Ocupación compaginada</strong><small>Consume un 150% del tiempo cuando coincide con otra actividad principal.</small></span></label></section><p v-if="trainingDraft.type === 'COURSE'" class="field-hint training-course-slots">Cursos disponibles: {{ trainingCourseSlots }} · Se concede uno por cada periodo de cuatro años, contando también el periodo parcial final.</p><section class="modal-section-panel"><h4 class="modal-section-title">Atributos desarrollados</h4><div class="training-form-grid training-attributes-grid"><label class="modal-field">Principal<select v-model="trainingDraft.primaryAttribute"><option value="">Sin atributo</option><option v-for="attribute in trainingAttributeOptions" :key="`primary-${attribute.key}`" :value="attribute.key" :disabled="trainingAttributeDisabled(attribute.key, 'primary')">{{ attribute.label }}</option></select></label><label v-if="trainingDraft.type !== 'COURSE'" class="modal-field">Secundario<select v-model="trainingDraft.secondaryAttribute"><option value="">Sin atributo</option><option v-for="attribute in trainingAttributeOptions" :key="`secondary-${attribute.key}`" :value="attribute.key" :disabled="trainingAttributeDisabled(attribute.key, 'secondary')">{{ attribute.label }}</option></select></label><label v-if="trainingDraft.type !== 'COURSE'" class="modal-field">Terciario<select v-model="trainingDraft.tertiaryAttribute"><option value="">Sin atributo</option><option v-for="attribute in trainingAttributeOptions" :key="`tertiary-${attribute.key}`" :value="attribute.key" :disabled="trainingAttributeDisabled(attribute.key, 'tertiary')">{{ attribute.label }}</option></select></label></div></section><section v-if="trainingPreview" class="training-preview modal-section-panel"><h4 class="modal-section-title">Vista previa del cálculo</h4><p><strong><template v-if="trainingDraft.type === 'COURSE'">No consume intervalo de tiempo</template><template v-else>{{ trainingPreview.humanYears }} años humanos equivalentes</template></strong></p><ul><li v-for="modifier in trainingPreview.modifiers" :key="modifier.attributeKey"><strong>{{ attributeLabels[modifier.attributeKey] || modifier.attributeKey }}</strong>: +{{ modifier.value }} <span>(base +{{ modifier.baseValue }}, coincidencias previas: {{ modifier.previousSelections }})</span></li></ul></section><div class="modal-actions"><button class="button button-primary" type="submit" :disabled="trainingLoading">{{ trainingLoading ? 'Guardando…' : 'Guardar actividad' }}</button><button class="button button-quiet" type="button" @click="resetTrainingDraft(); showTrainingForm=false">Limpiar</button></div></form></div><footer class="modal-actions"><button class="button button-primary" type="button" @click="showTrainingModal=false">Cerrar</button></footer></section></div>

      <section v-if="sheetView === 'sheet'" class="sheet-content" aria-labelledby="sheet-title">

<div class="sheet-heading"><div><p class="eyebrow accent">HOJA DE PERSONAJE</p><h2 id="sheet-title">{{ character.name }}</h2><p v-if="levelError" class="error-banner" role="alert">{{ levelError }}</p></div><div class="sheet-level"><span>Nivel</span><strong>{{ level }}</strong><small @dblclick="editing && (showExperienceModal=true, experienceError='')" title="Doble clic para añadir experiencia">{{ character.experience }} PX</small><div class="level-actions"><button v-if="trainingData.enabled" class="button button-quiet" type="button" @click="showTrainingModal=true; showTrainingForm=false">Formación</button><template v-if="editing"><button class="button button-quiet" type="button" :disabled="!canLevelUp || levelBusy" @click="openLevelUp()">Subir nivel</button><button class="button button-quiet" type="button" :disabled="!canLevelUpAll || levelBusy" @click="openLevelUp(true)">Subir varios niveles</button><button class="button button-quiet" type="button" @click="showExperienceModal=true; experienceError=''">Añadir experiencia</button></template></div></div></div>
<section class="sheet-panel"><h3>Atributos Mayores</h3><div class="value-grid attributes-grid major-attributes-grid"><div v-for="[key, value] in majorAttributes" :key="key" class="attribute-card"><button class="value-row attribute-row attribute-clickable attribute-detail-trigger" type="button" @click="openAttributeDetail(key)"><span>{{ attributeLabels[key] || key }}</span><strong>{{ displayedAttributeTotal(key, value) }}</strong></button><div class="attribute-card-footer"><button class="attribute-roll-trigger" type="button" :aria-label="`Tirar D10 de ${attributeLabels[key] || key}`" @click="openAttributeRoll(key, value, true)"><span class="attribute-roll-icon" aria-hidden="true"><img src="/diceD10.png" alt=""></span></button><small class="attribute-bonus">+{{ oneBonus(key, displayedAttributeTotal(key, value), true) }} · +{{ d6Bonus(key, displayedAttributeTotal(key, value), true) }}D6</small></div></div><p v-if="!Object.keys(attributes).length" class="sheet-muted">Sin atributos registrados.</p></div></section>

        <section class="sheet-panel"><div class="sheet-panel-heading"><h3>Atributos Menores</h3><button v-if="editing" class="button button-quiet" type="button" @click="showMinorModal=true">Añadir atributo menor</button></div><div class="value-grid attributes-grid"><div v-for="[key, value] in minorAttributes" :key="key" class="attribute-card"><button class="value-row attribute-row attribute-clickable attribute-detail-trigger" type="button" @click="openAttributeDetail(key)"><span>{{ attributeLabels[key] || customMinor(key)?.name || key }}</span><strong>{{ displayedAttributeTotal(key, customMinor(key)?.total ?? value) }}</strong></button><div class="attribute-card-footer"><button class="attribute-roll-trigger" type="button" :aria-label="`Tirar D10 de ${attributeLabels[key] || customMinor(key)?.name || key}`" @click="openAttributeRoll(key, value, false)"><span class="attribute-roll-icon" aria-hidden="true"><img src="/diceD10.png" alt=""></span></button><small class="attribute-bonus">+{{ maxBonus(key, displayedAttributeTotal(key, customMinor(key)?.total ?? value), false) }} · +{{ maxD6(key, displayedAttributeTotal(key, customMinor(key)?.total ?? value), false) }}D6</small></div></div></div></section>

        <section class="sheet-panel"><h3>Genética</h3><div class="genetic-groups"><div v-for="group in geneticGroups" :key="group.label" class="genetic-group"><h4>{{ group.label }}</h4><div class="value-grid genetic-grid"><div v-for="key in group.keys" :key="key" class="value-row attribute-clickable" role="button" tabindex="0" @click="openAttributeDetail(key)" @keydown.enter="openAttributeDetail(key)" @keydown.space.prevent="openAttributeDetail(key)"><span>{{ geneticLabels[key] || key }}</span><strong>{{ geneticTotals[key] ?? 0 }}</strong></div></div></div></div><p v-if="!Object.keys(genetics).length" class="sheet-muted">Sin valores genéticos.</p></section>

        <section class="sheet-panel"><h3>Valores calculados</h3><div class="value-grid attributes-grid derived-stats-grid"><div v-for="key in derivedStatKeys" :key="key" class="value-row attribute-row attribute-clickable" role="button" tabindex="0" @click="openAttributeDetail(key)" @keydown.enter="openAttributeDetail(key)" @keydown.space.prevent="openAttributeDetail(key)"><span>{{ derivedStats[key]?.name || key }}</span><strong>{{ displayedAttributeTotal(key, derivedStats[key]?.total ?? 0) }}</strong><small>{{ derivedStats[key]?.formula || '—' }}</small></div></div></section>

      </section>

      <section v-else-if="sheetView === 'inventory'" class="sheet-content inventory-content" aria-labelledby="inventory-title">
        <div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO</p><h2 id="inventory-title">Equipamiento</h2><p class="modal-copy">Armas equipadas y otros objetos personales.</p></div><button class="button button-primary" type="button" @click="openInventoryType">＋ Añadir objeto</button></div>
         <p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p><p v-if="inventoryLoading" class="sheet-state">Cargando inventario…</p>
         <section class="weapon-section"><div class="sheet-panel-heading"><h3>Armas</h3><span class="field-hint">3 pequeñas · 2 medianas · 1 universal</span></div><div class="weapon-slots"><article v-for="slot in weaponSlots" :key="slot.value" class="weapon-slot"><header><strong>{{ slot.label }}</strong><button v-if="!weaponAt(slot.value)" class="button button-quiet" type="button" @click="openNewWeapon(slot.value)">Añadir</button></header><template v-if="weaponAt(slot.value)"><div class="inventory-item weapon-card" role="button" tabindex="0" :aria-label="'Ver detalles de ' + weaponAt(slot.value)!.name" @click="openWeapon(weaponAt(slot.value)!)" @keydown.enter.prevent="openWeapon(weaponAt(slot.value)!)" @keydown.space.prevent="openWeapon(weaponAt(slot.value)!)"><img v-if="weaponAt(slot.value)!.imageUrl" class="weapon-thumbnail" :src="weaponImage(weaponAt(slot.value)!)" alt=""><button class="button weapon-card-shoot-button" type="button" :aria-label="'Disparar ' + weaponAt(slot.value)!.name" title="Disparar arma" :disabled="weaponShooting || weaponAt(slot.value)!.loadedBullets <= 0" @click.stop="openShoot(weaponAt(slot.value)!)" @keydown.stop>Disparar</button><button class="button weapon-card-reload-button" type="button" :aria-label="'Recargar ' + weaponAt(slot.value)!.name" title="Recargar arma" :disabled="weaponReloading" @click.stop="reloadWeapon(weaponAt(slot.value)!)" @keydown.stop>{{ weaponReloading ? 'Recargando…' : 'Recargar' }}</button><span><strong>{{ weaponAt(slot.value)!.name }}</strong><small>{{ weaponTypes.find(type=>type.value===weaponAt(slot.value)!.weaponType)?.label || weaponAt(slot.value)!.weaponType }}</small><small class="weapon-card-stats">Balas cargadas {{ weaponAt(slot.value)!.loadedBullets }}/{{ weaponAt(slot.value)!.capacity }} · Puntería {{ weaponAt(slot.value)!.aim ?? '—' }} · Daño {{ weaponDamage(weaponAt(slot.value)!) }} · {{ weaponRate(weaponAt(slot.value)!.rate) }}</small></span></div></template><p v-else class="sheet-muted">Hueco libre</p></article></div></section>
        <section class="weapon-section protection-section"><div class="sheet-panel-heading"><h3>Protecciones</h3></div><div class="weapon-slots protection-slots">
          <article v-for="slot in armorSlots" :key="slot.value" class="weapon-slot protection-slot"><header><strong>Armadura {{ slot.label.toLowerCase() }}</strong><button v-if="!armorAtSlot(slot.value)" class="button button-quiet" type="button" @click="openArmorDetail(undefined, slot.value)">Añadir</button></header><template v-if="armorAtSlot(slot.value)"><button class="inventory-item weapon-card armor-card" type="button" @click="openArmorDetail(armorAtSlot(slot.value)!)"><img v-if="armorAtSlot(slot.value)!.imageUrl" class="weapon-thumbnail" :src="armorAtSlot(slot.value)!.imageUrl!" :alt="armorAtSlot(slot.value)!.name"><span><strong>{{ armorAtSlot(slot.value)!.name }}</strong><small>Armadura activa</small><small class="weapon-card-stats">RD {{ armorAtSlot(slot.value)!.rdBySlot[slot.value] }} · Armadura {{ armorAtSlot(slot.value)!.armorBySlot[slot.value] }}</small></span></button></template><p v-else class="sheet-muted">Hueco libre</p></article>
          <article class="weapon-slot protection-slot"><header><strong>Escudo</strong><button v-if="!physicalShields.length" class="button button-quiet" type="button" @click="openPhysicalShieldDetail()">Añadir</button></header><template v-if="physicalShields.length"><button class="inventory-item weapon-card shield-card" type="button" @click="openPhysicalShieldDetail(physicalShields[0])"><img v-if="physicalShields[0].imageUrl" class="weapon-thumbnail" :src="physicalShields[0].imageUrl" :alt="physicalShields[0].name"><span><strong>{{ physicalShields[0].name }}</strong><small>Escudo físico activo</small><small class="weapon-card-stats">RD {{ physicalShields[0].rd }} · Armadura {{ physicalShields[0].armor }} · Defensa {{ physicalShields[0].defense }}</small></span></button></template><p v-else class="sheet-muted">Hueco libre</p></article>
          <article class="weapon-slot protection-slot"><header><strong>Escudo de energía</strong><button v-if="!shields.length" class="button button-quiet" type="button" @click="openShieldDetail()">Añadir</button></header><template v-if="shields.length"><button class="inventory-item weapon-card shield-card" type="button" @click="openShieldDetail(shields[0])"><img v-if="shields[0].imageUrl" class="weapon-thumbnail" :src="shields[0].imageUrl" :alt="shields[0].name"><span><strong>{{ shields[0].name }}</strong><small>Escudo de energía activo</small><small class="weapon-card-stats">{{ shields[0].hitPoints }} PV</small></span></button></template><p v-else class="sheet-muted">Hueco libre</p></article>
        </div></section>
        <section class="weapon-section ammunition-section"><div class="sheet-panel-heading"><h3>Munición</h3><button class="button button-quiet" type="button" @click="openNewAmmunition">＋ Añadir munición</button></div><p v-if="!ammunition.length" class="sheet-state">No hay munición registrada.</p><div v-else class="ammunition-list"><article v-for="item in ammunition" :key="item.id" class="ammunition-item"><button class="inventory-item ammunition-summary" type="button" @click="openAmmunition(item)"><span><strong>{{ item.caliber }}</strong><small>Munición disponible</small></span><strong>{{ item.quantity }} ud.</strong></button><div class="ammunition-actions" aria-label="Descontar munición"><button v-for="amount in [-1, -5, -10]" :key="amount" class="button button-quiet" type="button" :disabled="ammunitionDeleting || item.quantity < Math.abs(amount)" @click="decrementAmmunition(item, amount as -1 | -5 | -10)">{{ amount }}</button></div></article></div></section>
        <section class="weapon-section"><div class="sheet-panel-heading"><h3>Otros objetos</h3><button class="button button-quiet" type="button" @click="openNewOtherItem">＋ Añadir objeto</button></div><p v-if="!otherInventory.length" class="sheet-state">No hay objetos registrados.</p><div v-else class="inventory-list"><button v-for="item in otherInventory" :key="item.id" class="inventory-item" type="button" @click="openOtherItem(item)"><span><strong>{{ item.name }}</strong><small>{{ item.location || 'Sin localización' }} · {{ item.quantity }} ud.</small></span><strong>{{ item.unitValue ?? 0 }}</strong></button></div></section>
      </section>

      <section v-else-if="sheetView === 'inventory-type'" class="sheet-content inventory-content" aria-labelledby="inventory-type-title">
        <div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO</p><h2 id="inventory-type-title">Añadir objeto</h2><p class="modal-copy">Selecciona qué tipo de objeto quieres añadir al inventario.</p></div></div>
        <div class="inventory-type-grid"><button class="inventory-type-card" type="button" @click="openNewWeapon()"><strong>Arma</strong><span>Una de las armas que lleva equipada el personaje.</span></button><button class="inventory-type-card" type="button" @click="openArmorDetail()"><strong>Armadura</strong><span>Protección con RD y Armadura por hueco.</span></button><button class="inventory-type-card" type="button" @click="openShieldDetail()"><strong>Escudo de energía</strong><span>Protección activa con PV.</span></button><button class="inventory-type-card" type="button" @click="openPhysicalShieldDetail()"><strong>Escudo</strong><span>Escudo físico con RD, Armadura y Defensa.</span></button><button class="inventory-type-card" type="button" @click="openNewAmmunition()"><strong>Munición</strong><span>Una reserva agrupada por calibre.</span></button><button class="inventory-type-card" type="button" @click="openNewOtherItem()"><strong>Otro</strong><span>Un objeto personal que no es un arma.</span></button></div>
        <div class="modal-actions"><button class="button button-quiet" type="button" @click="sheetView='inventory'">Cancelar</button></div>
      </section>

      <section v-else-if="sheetView === 'weapon-choice'" class="sheet-content inventory-content"><div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · ARMAS</p><h2>Añadir arma</h2><p class="modal-copy">Elige una arma de lista o crea una plantilla personalizada.</p></div></div><div class="inventory-type-grid"><button class="inventory-type-card" type="button" @click="openWeaponCatalog"><strong>Lista</strong><span>Busca las armas oficiales y las personalizadas guardadas.</span></button><button class="inventory-type-card" type="button" @click="sheetView='weapon-detail'"><strong>Personalizado</strong><span>Crea un arma reutilizable con su propia imagen.</span></button></div><div class="modal-actions"><button class="button button-quiet" type="button" @click="sheetView='inventory'">Cancelar</button></div></section>

      <section v-else-if="sheetView === 'weapon-catalog'" class="sheet-content inventory-content"><div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · ARMAS</p><h2>Lista de armas</h2><p class="modal-copy">Solo se muestran armas compatibles con {{ weaponSlots.find(slot=>slot.value===catalogSlot)?.label }}.</p></div></div><p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p><div class="inventory-form-grid"><label class="modal-field"><span>Buscar por nombre</span><input v-model="catalogSearch" @input="loadWeaponCatalog" autofocus></label><label class="modal-field"><span>Tipo</span><select v-model="catalogType" @change="loadWeaponCatalog"><option value="">Todos</option><option v-for="type in weaponTypes" :key="type.value" :value="type.value">{{ type.label }}</option></select></label></div><p v-if="catalogLoading" class="sheet-state">Cargando armas…</p><div v-else class="character-grid weapon-catalog-grid"><button v-for="item in catalogWeapons" :key="item.id" class="weapon-catalog-card character-card" type="button" @click="selectCatalogWeapon(item)"><div class="portrait weapon-portrait"><img v-if="item.imageUrl" :src="weaponCatalogImage(item)" :alt="item.name"><span v-else>⚔</span></div><div class="character-info weapon-catalog-info"><p class="eyebrow accent">ARMA</p><h3>{{ item.name }}</h3><small class="weapon-catalog-type">{{ weaponTypes.find(type=>type.value===item.weaponType)?.label || item.weaponType }}</small><dl class="weapon-catalog-stats"><div><dt>Puntería</dt><dd>{{ item.aim ?? '—' }}</dd></div><div><dt>Daño</dt><dd :title="`Vital / Normal / Leve / Muy leve: ${item.damageVital} / ${item.damageNormal} / ${item.damageLight} / ${item.damageVeryLight}`">{{ weaponDamage(item) }}</dd></div><div class="weapon-catalog-rate"><dd>{{ weaponRate(item.rate) }}</dd></div></dl></div></button></div><p v-if="!catalogLoading && !catalogWeapons.length" class="sheet-state">No hay armas compatibles.</p><div class="modal-actions"><button class="button button-quiet" type="button" @click="sheetView='weapon-choice'">Volver</button></div></section>

      <section v-else-if="sheetView === 'weapon-detail'" class="sheet-content inventory-content" aria-labelledby="weapon-detail-title">
        <div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · ARMAS</p><h2 id="weapon-detail-title">{{ selectedWeapon ? 'Editar arma' : 'Añadir arma' }}</h2><p class="modal-copy">Completa las características del arma.</p></div></div>
        <p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p>
         <form class="inventory-form" @submit.prevent="saveWeapon"><div class="inventory-form-grid"><label class="modal-field"><span>Hueco</span><select v-model="weaponDraft.slot" :disabled="weaponSlotLocked || !!selectedWeapon" @change="onWeaponSlotChange"><option v-for="slot in weaponSlots" :key="slot.value" :value="slot.value" :disabled="!compatibleSlots(weaponDraft.size).includes(slot.value)">{{ slot.label }}</option></select></label><label class="modal-field"><span>Nombre</span><input v-model="weaponDraft.name" required maxlength="255" autofocus /></label><label class="modal-field"><span>Tipo</span><select v-model="weaponDraft.weaponType" :disabled="!!selectedWeapon?.catalogWeaponId"><option v-for="type in weaponTypes" :key="type.value" :value="type.value">{{ type.label }}</option></select></label></div><label v-if="!selectedWeapon" class="modal-field"><span>Imagen</span><input type="file" accept="image/png,image/jpeg,image/gif,image/webp" @change="onCustomWeaponImage"><img v-if="customImageUrl" class="weapon-image-preview" :src="customImageUrl" alt="Vista previa"></label><label v-if="selectedWeapon" class="modal-field weapon-move"><span>Mover a</span><select :value="selectedWeapon.slot" :disabled="weaponMoving" @change="moveWeapon(selectedWeapon, ($event.target as HTMLSelectElement).value)"><option v-for="target in weaponMoveTargets(selectedWeapon)" :key="target" :value="target">{{ weaponSlots.find(s=>s.value===target)?.label }}</option></select></label><div class="inventory-form-grid"><label class="modal-field"><span>Tamaño</span><select v-model="weaponDraft.size" @change="onWeaponSizeChange"><option v-for="size in weaponSizes" :key="size.value" :value="size.value" :disabled="!compatibleSizes(weaponDraft.slot).includes(size.value)">{{ size.label }}</option></select></label><label class="modal-field"><span>Alcance</span><input v-model.number="weaponDraft.range" type="number" min="0" step="any" required /></label><label class="modal-field"><span>Recarga</span><input v-model.number="weaponDraft.reload" type="number" min="0" step="any" required /></label><label class="modal-field"><span>Cadencia</span><input v-model="weaponDraft.rate" required /></label></div><section class="modal-section-panel"><h3 class="modal-section-title">Daño</h3><div class="weapon-damage-grid"><label class="modal-field"><span>Vital</span><input v-model.number="weaponDraft.damageVital" type="number" min="0" step="any" required /></label><label class="modal-field"><span>Normal</span><input v-model.number="weaponDraft.damageNormal" type="number" min="0" step="any" required /></label><label class="modal-field"><span>Leve</span><input v-model.number="weaponDraft.damageLight" type="number" min="0" step="any" required /></label><label class="modal-field"><span>Muy leve</span><input v-model.number="weaponDraft.damageVeryLight" type="number" min="0" step="any" required /></label></div></section><div class="inventory-form-grid"><label class="modal-field"><span>Puntería</span><input v-model.number="weaponDraft.aim" type="number" step="any" /></label><label class="modal-field"><span>Fuego automático</span><input v-model="weaponDraft.automaticFire" /></label><label class="modal-field"><span>Capacidad</span><input v-model.number="weaponDraft.capacity" type="number" min="1" step="1" required /></label><label class="modal-field"><span>Balas cargadas</span><input v-model.number="weaponDraft.loadedBullets" type="number" min="0" :max="weaponDraft.capacity" step="1" required /></label><label class="modal-field"><span>Calibre</span><input v-model="weaponDraft.caliber" required /></label></div><label class="modal-field"><span>Regla extra</span><textarea v-model="weaponDraft.extraRule" rows="4"></textarea></label><div class="modal-actions"><button class="button button-primary" type="submit" :disabled="weaponSaving">{{ weaponSaving ? 'Guardando…' : 'Guardar arma' }}</button><button class="button button-quiet" type="button" @click="closeWeaponDetail">Cancelar</button><button v-if="selectedWeapon" class="button button-danger" type="button" :disabled="weaponDeleting" @click="deleteWeapon">{{ weaponDeleting ? 'Borrando…' : 'Borrar arma' }}</button></div></form>
      </section>

      <section v-else-if="sheetView === 'inventory-detail'" class="sheet-content inventory-content" aria-labelledby="inventory-detail-title">
        <div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · OTROS</p><h2 id="inventory-detail-title">{{ selectedOtherItem ? 'Editar objeto' : 'Añadir objeto' }}</h2><p class="modal-copy">Completa los datos del objeto que lleva {{ character.name }}.</p></div></div>
        <p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p>
        <form class="inventory-form" @submit.prevent="saveOtherItem">
          <label class="modal-field"><span>Nombre</span><input v-model="inventoryDraft.name" required maxlength="255" autofocus /></label>
          <label class="modal-field"><span>Descripción</span><textarea v-model="inventoryDraft.description" rows="4" maxlength="4000"></textarea></label>
          <div class="inventory-form-grid"><label class="modal-field"><span>Localización</span><input v-model="inventoryDraft.location" maxlength="255" placeholder="Ej. Mochila" /></label><label class="modal-field"><span>Cantidad</span><input v-model.number="inventoryDraft.quantity" type="number" min="1" step="1" required /></label><label class="modal-field"><span>Valor unitario</span><input v-model.number="inventoryDraft.unitValue" type="number" min="0" step="0.01" /></label></div>
          <div class="modal-actions"><button class="button button-primary" type="submit" :disabled="inventorySaving || !inventoryDraft.name.trim() || Number(inventoryDraft.quantity) < 1">{{ inventorySaving ? 'Guardando…' : 'Guardar objeto' }}</button><button class="button button-quiet" type="button" @click="closeInventoryDetail">Cancelar</button><button v-if="selectedOtherItem" class="button button-danger" type="button" :disabled="inventoryDeleting" @click="deleteOtherItem">{{ inventoryDeleting ? 'Borrando…' : 'Borrar objeto' }}</button></div>
        </form>
      </section>

      <section v-else-if="sheetView === 'abilities'" class="sheet-content abilities-content" aria-labelledby="abilities-title">
        <div class="sheet-heading"><div><p class="eyebrow accent">HABILIDADES</p><h2 id="abilities-title">{{ character.name }}</h2><p class="modal-copy">Todas las habilidades obtenidas en la última versión cerrada.</p></div><strong class="ability-count">{{ obtainedAbilities.length }}</strong></div>
        <p v-if="abilityCatalogLoading" class="sheet-state">Cargando habilidades…</p>
        <p v-else-if="abilityCatalogError" class="error-banner" role="alert">{{ abilityCatalogError }}</p>
          <div v-else-if="obtainedAbilities.length" class="abilities-grid"><article v-for="ability in obtainedAbilities" :key="ability.name" class="ability-card"><button class="ability-card-main" type="button" @click="openAbilityDetail(ability)">{{ ability.name }}</button><button v-if="canRollAbility(ability)" class="ability-roll-trigger" type="button" :aria-label="`Tirar D10 para ${ability.name}`" @click="openAbilityRoll(ability)"><img src="/diceD10.png" alt="" aria-hidden="true" /></button></article></div>
        <p v-else class="sheet-state">No hay habilidades obtenidas en una versión cerrada.</p>
      </section>

      <section v-else-if="sheetView === 'ammunition-detail'" class="sheet-content inventory-content" aria-labelledby="ammunition-detail-title">
        <div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · MUNICIÓN</p><h2 id="ammunition-detail-title">{{ selectedAmmunition ? 'Editar munición' : 'Añadir munición' }}</h2><p class="modal-copy">La munición se agrupa automáticamente por calibre.</p></div></div>
        <p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p>
        <form class="inventory-form" @submit.prevent="saveAmmunition">
          <label class="modal-field"><span>Calibre</span><select v-model="ammunitionDraft.caliber" :disabled="!!selectedAmmunition" required><option value="" disabled>Selecciona un calibre</option><option v-for="caliber in ammunitionCalibers" :key="caliber" :value="caliber">{{ caliber }}</option></select></label>
          <label class="modal-field"><span>Cantidad</span><input v-model.number="ammunitionDraft.quantity" type="number" min="1" step="1" required></label>
          <p v-if="!ammunitionCalibers.length" class="sheet-state">No hay calibres disponibles. Añade un arma con calibre para habilitar munición.</p>
          <div class="modal-actions"><button class="button button-primary" type="submit" :disabled="ammunitionSaving || !ammunitionDraft.caliber || Number(ammunitionDraft.quantity) < 1">{{ ammunitionSaving ? 'Guardando…' : 'Guardar munición' }}</button><button class="button button-quiet" type="button" @click="closeAmmunitionDetail">Cancelar</button></div>
        </form>
      </section>

        <section v-else-if="sheetView === 'armor-detail'" class="sheet-content inventory-content"><div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · ARMADURA</p><h2>{{ selectedArmor ? 'Editar armadura' : 'Añadir armadura' }}</h2></div></div><p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p><form class="inventory-form" @submit.prevent="saveArmor"><label class="modal-field"><span>Nombre</span><input v-model="armorDraft.name" required maxlength="255" autofocus></label><label class="modal-field"><span>Descripción</span><textarea v-model="armorDraft.description" rows="3"></textarea></label><label class="modal-field"><span>Imagen</span><input type="file" accept="image/png,image/jpeg,image/gif,image/webp" @change="onProtectiveImage($event,'armor')"><img v-if="armorDraft.imageUrl" class="weapon-image-preview" :src="armorDraft.imageUrl" alt="Vista previa"></label><section class="modal-section-panel"><h3 class="modal-section-title">Huecos y valores</h3><div v-for="slot in armorSlots" :key="slot.value" class="armor-slot-editor"><label><input type="checkbox" :checked="armorDraft.slots.includes(slot.value)" :disabled="armorSlotUnavailable(slot.value)" @change="toggleArmorSlot(slot.value)"> {{ slot.label }}<small v-if="armorSlotUnavailable(slot.value)">Ya ocupado</small></label><template v-if="armorDraft.slots.includes(slot.value)"><label class="modal-field"><span>RD</span><input v-model.number="armorDraft.rdBySlot[slot.value]" type="number" min="0" max="999" required></label><label class="modal-field"><span>Armadura</span><input v-model.number="armorDraft.armorBySlot[slot.value]" type="number" min="0" max="9999" required></label></template></div></section><div class="modal-actions"><button class="button button-primary" type="submit" :disabled="armorSaving || !armorDraft.slots.length">{{ armorSaving ? 'Guardando…' : 'Guardar armadura' }}</button><button class="button button-quiet" type="button" @click="sheetView='inventory'">Cancelar</button><button v-if="selectedArmor" class="button button-danger" type="button" @click="deleteArmor">Borrar</button></div></form></section>

      <section v-else-if="sheetView === 'shield-detail'" class="sheet-content inventory-content"><div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · ESCUDO DE ENERGÍA</p><h2>{{ selectedShield ? 'Editar escudo de energía' : 'Añadir escudo de energía' }}</h2></div></div><p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p><form class="inventory-form" @submit.prevent="saveShield"><label class="modal-field"><span>Nombre</span><input v-model="shieldDraft.name" required maxlength="255" autofocus></label><label class="modal-field"><span>Descripción</span><textarea v-model="shieldDraft.description" rows="3"></textarea></label><label class="modal-field"><span>PV</span><input v-model.number="shieldDraft.hitPoints" type="number" min="0" required></label><label class="modal-field"><span>Imagen</span><input type="file" accept="image/png,image/jpeg,image/gif,image/webp" @change="onProtectiveImage($event,'shield')"><img v-if="shieldDraft.imageUrl" class="weapon-image-preview" :src="shieldDraft.imageUrl" alt="Vista previa"></label><div class="modal-actions"><button class="button button-primary" type="submit" :disabled="shieldSaving">{{ shieldSaving ? 'Guardando…' : 'Guardar escudo' }}</button><button class="button button-quiet" type="button" @click="sheetView='inventory'">Cancelar</button><button v-if="selectedShield" class="button button-danger" type="button" @click="deleteShield">Borrar</button></div></form></section>
      <section v-else-if="sheetView === 'physical-shield-detail'" class="sheet-content inventory-content"><div class="sheet-heading"><div><p class="eyebrow accent">INVENTARIO · ESCUDO</p><h2>{{ selectedPhysicalShield ? 'Editar escudo' : 'Añadir escudo' }}</h2></div></div><p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p><form class="inventory-form" @submit.prevent="savePhysicalShield"><label class="modal-field"><span>Nombre</span><input v-model="physicalShieldDraft.name" required maxlength="255" autofocus></label><label class="modal-field"><span>Descripción</span><textarea v-model="physicalShieldDraft.description" rows="3"></textarea></label><div class="inventory-form-grid"><label class="modal-field"><span>RD</span><input v-model.number="physicalShieldDraft.rd" type="number" min="0" max="999" required></label><label class="modal-field"><span>Armadura</span><input v-model.number="physicalShieldDraft.armor" type="number" min="0" max="9999" required></label><label class="modal-field"><span>Defensa</span><input v-model.number="physicalShieldDraft.defense" type="number" required></label></div><label class="modal-field"><span>Otros efectos</span><textarea v-model="physicalShieldDraft.otherEffects" rows="3"></textarea></label><label class="modal-field"><span>Imagen</span><input type="file" accept="image/png,image/jpeg,image/gif,image/webp" @change="onProtectiveImage($event,'physicalShield')"><img v-if="physicalShieldDraft.imageUrl" class="weapon-image-preview" :src="physicalShieldDraft.imageUrl" alt="Vista previa"></label><div class="modal-actions"><button class="button button-primary" type="submit" :disabled="shieldSaving">{{ shieldSaving ? 'Guardando…' : 'Guardar escudo' }}</button><button class="button button-quiet" type="button" @click="sheetView='inventory'">Cancelar</button><button v-if="selectedPhysicalShield" class="button button-danger" type="button" @click="deletePhysicalShield">Borrar</button></div></form></section>
    </div>


    <div v-if="showShieldDetailModal && selectedShield" class="modal-backdrop" @click.self="closeShieldDetailModal">
      <section class="modal-card weapon-detail-modal shield-detail-modal" role="dialog" aria-modal="true" aria-labelledby="shield-detail-modal-title">
        <header class="modal-header"><div><p class="eyebrow accent">INVENTARIO · ESCUDO DE ENERGÍA</p><h2 id="shield-detail-modal-title">{{ selectedShield.name }}</h2><p class="modal-copy">Escudo de energía activo</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeShieldDetailModal">×</button></header>
        <div class="modal-body weapon-detail-body"><div class="weapon-detail-hero"><div class="weapon-detail-image shield-detail-image"><img v-if="selectedShield.imageUrl" :src="selectedShield.imageUrl" :alt="selectedShield.name"><span v-else>🛡</span></div><dl class="weapon-detail-stats"><div><dt>Protección</dt><dd>{{ selectedShield.hitPoints }} PV</dd></div></dl></div><p v-if="selectedShield.description" class="weapon-detail-rule">{{ selectedShield.description }}</p></div>
        <footer class="modal-actions"><button class="button button-primary" type="button" @click="editSelectedShield">Modificar</button><button class="button button-quiet" type="button" @click="closeShieldDetailModal">Cerrar</button><button class="button button-danger" type="button" @click="deleteShield">Borrar</button></footer>
      </section>
    </div>

    <div v-if="showPhysicalShieldDetailModal && selectedPhysicalShield" class="modal-backdrop" @click.self="closePhysicalShieldDetailModal"><section class="modal-card weapon-detail-modal shield-detail-modal" role="dialog" aria-modal="true" aria-labelledby="physical-shield-detail-modal-title"><header class="modal-header"><div><p class="eyebrow accent">INVENTARIO · ESCUDO</p><h2 id="physical-shield-detail-modal-title">{{ selectedPhysicalShield.name }}</h2><p class="modal-copy">Escudo físico activo</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closePhysicalShieldDetailModal">×</button></header><div class="modal-body weapon-detail-body"><div class="weapon-detail-hero"><div class="weapon-detail-image shield-detail-image"><img v-if="selectedPhysicalShield.imageUrl" :src="selectedPhysicalShield.imageUrl" :alt="selectedPhysicalShield.name"><span v-else>🛡</span></div><dl class="weapon-detail-stats"><div><dt>RD</dt><dd>{{ selectedPhysicalShield.rd }}</dd></div><div><dt>Armadura</dt><dd>{{ selectedPhysicalShield.armor }}</dd></div><div><dt>Defensa</dt><dd>{{ selectedPhysicalShield.defense }}</dd></div></dl></div><p v-if="selectedPhysicalShield.description" class="weapon-detail-rule">{{ selectedPhysicalShield.description }}</p><p v-if="selectedPhysicalShield.otherEffects" class="weapon-detail-rule"><strong>Otros efectos:</strong> {{ selectedPhysicalShield.otherEffects }}</p></div><footer class="modal-actions"><button class="button button-primary" type="button" @click="editSelectedPhysicalShield">Modificar</button><button class="button button-quiet" type="button" @click="closePhysicalShieldDetailModal">Cerrar</button><button class="button button-danger" type="button" @click="deletePhysicalShield">Borrar</button></footer></section></div>

    <div v-if="showArmorDetailModal && selectedArmor" class="modal-backdrop" @click.self="closeArmorDetailModal">
      <section class="modal-card weapon-detail-modal armor-detail-modal" role="dialog" aria-modal="true" aria-labelledby="armor-detail-modal-title">
        <header class="modal-header"><div><p class="eyebrow accent">INVENTARIO · ARMADURA</p><h2 id="armor-detail-modal-title">{{ selectedArmor.name }}</h2><p class="modal-copy">Armadura activa · {{ selectedArmor.slots.map(slot => armorSlots.find(item => item.value === slot)?.label).join(' · ') }}</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeArmorDetailModal">×</button></header>
        <div class="modal-body weapon-detail-body"><div class="weapon-detail-hero"><div class="weapon-detail-image armor-detail-image"><img v-if="selectedArmor.imageUrl" :src="selectedArmor.imageUrl" :alt="selectedArmor.name"><span v-else>🛡</span></div><dl class="weapon-detail-stats"><div v-for="slot in selectedArmor.slots" :key="slot"><dt>{{ armorSlots.find(item => item.value === slot)?.label }}</dt><dd>RD {{ selectedArmor.rdBySlot[slot] }} · Armadura {{ selectedArmor.armorBySlot[slot] }}</dd></div></dl></div><p v-if="selectedArmor.description" class="weapon-detail-rule">{{ selectedArmor.description }}</p></div>
        <footer class="modal-actions"><button class="button button-primary" type="button" @click="editSelectedArmor">Modificar</button><button class="button button-quiet" type="button" @click="closeArmorDetailModal">Cerrar</button><button class="button button-danger" type="button" @click="deleteArmor">Borrar</button></footer>
      </section>
    </div>

     <div v-if="showShootModal && shootWeaponTarget" class="modal-backdrop" @click.self="closeShoot">
       <section class="modal-card shoot-modal" role="dialog" aria-modal="true" aria-labelledby="shoot-modal-title">
         <header class="modal-header"><div><p class="eyebrow accent">INVENTARIO · ARMA</p><h2 id="shoot-modal-title">Disparar {{ shootWeaponTarget.name }}</h2><p class="modal-copy">Balas cargadas: {{ shootWeaponTarget.loadedBullets }}/{{ shootWeaponTarget.capacity }}</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeShoot">×</button></header>
         <div class="modal-body"><div class="shoot-options"><button v-for="option in shootOptions(shootWeaponTarget)" :key="option.label" class="button button-primary" type="button" :disabled="weaponShooting" @click="shootWeapon(shootWeaponTarget, option.shots, option.automatic)">{{ option.label }}<small>{{ option.shots }} bala{{ option.shots === 1 ? '' : 's' }}</small></button><p v-if="!shootOptions(shootWeaponTarget).length" class="sheet-muted">No hay balas cargadas.</p></div><p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p></div>
         <footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeShoot">Cancelar</button></footer>
       </section>
     </div>
    <div v-if="showWeaponAimRollModal && weaponAimRoll" class="modal-backdrop weapon-aim-roll-backdrop" @click.self="closeWeaponAimRolls">
      <section class="modal-card weapon-aim-roll-modal" role="dialog" aria-modal="true" aria-labelledby="weapon-aim-roll-title" tabindex="-1" @keydown.esc="closeWeaponAimRolls">
        <header class="modal-header">
          <div>
            <p class="eyebrow accent">TIRADA DE PUNTERÍA</p>
            <h2 id="weapon-aim-roll-title">{{ weaponAimRoll.weaponName }}</h2>
            <p class="modal-copy">Puntería {{ weaponAimRoll.score }} · +{{ weaponAimRoll.plusOne }} · +{{ weaponAimRoll.plusD6 }}D6 · arma +{{ weaponAimRoll.weaponAim }}</p>
          </div>
          <button id="weapon-aim-roll-close" class="modal-close" type="button" aria-label="Cerrar tiradas de puntería" @click="closeWeaponAimRolls">×</button>
        </header>
        <div class="modal-body weapon-aim-roll-body">
          <p class="attribute-roll-help">Cada disparo se tira por separado. Selecciona 1 D10 y 2 D6; la puntería del arma se suma como bonificación adicional.</p>
          <div class="weapon-aim-roll-list">
            <article v-for="(roll, index) in weaponAimRoll.rolls" :key="roll.id" class="weapon-aim-roll-card" :aria-labelledby="`weapon-aim-roll-${roll.id}-title`">
              <header class="weapon-aim-roll-heading">
                <h3 :id="`weapon-aim-roll-${roll.id}-title`">Disparo {{ index + 1 }}</h3>
                <button class="button button-quiet" type="button" :aria-label="`Volver a tirar el disparo ${index + 1}`" @click="rerollWeaponAimRoll(roll.id)">Volver a tirar</button>
              </header>
              <div class="weapon-aim-roll-layout">
                <section class="attribute-roll-dice-group" :aria-labelledby="`weapon-aim-roll-${roll.id}-d10-label`">
                  <h4 :id="`weapon-aim-roll-${roll.id}-d10-label`">D10</h4>
                  <div class="attribute-roll-dice">
                    <button v-for="die in roll.dice.filter(item => item.type === 'd10')" :key="die.id" class="roll-die" :class="{ selected: die.selected }" type="button" :aria-pressed="die.selected" :aria-label="`Disparo ${index + 1}, D10: ${die.value}${die.selected ? ', seleccionado' : ', no seleccionado'}`" @click="toggleWeaponAimRollDie(roll.id, die.id)">
                      <img :src="attributeRollDieImage(die)" alt="">
                      <strong>{{ die.value }}</strong>
                    </button>
                  </div>
                </section>
                <section class="attribute-roll-dice-group" :aria-labelledby="`weapon-aim-roll-${roll.id}-d6-label`">
                  <h4 :id="`weapon-aim-roll-${roll.id}-d6-label`">D6 disponibles</h4>
                  <div class="attribute-roll-dice">
                    <button v-for="die in roll.dice.filter(item => item.type === 'd6')" :key="die.id" class="roll-die" :class="{ selected: die.selected, disabled: die.disabled }" type="button" :disabled="die.disabled" :aria-pressed="die.selected" :aria-label="`Disparo ${index + 1}, D6: ${die.value}${die.disabled ? ', desactivado por sacar 1' : die.selected ? ', seleccionado' : ', no seleccionado'}`" @click="toggleWeaponAimRollDie(roll.id, die.id)">
                      <img :src="attributeRollDieImage(die)" alt="">
                      <strong>{{ die.value }}</strong>
                    </button>
                  </div>
                </section>
                <section class="attribute-roll-dice-group weapon-aim-roll-damage" :aria-labelledby="`weapon-aim-roll-${roll.id}-damage-label`">
                  <h4 :id="`weapon-aim-roll-${roll.id}-damage-label`">Daño</h4>
                  <div class="weapon-aim-roll-damage-content">
                    <div class="roll-die selected weapon-aim-roll-damage-die" role="img" :aria-label="`Disparo ${index + 1}, D10 de daño: ${roll.damageD10.value}, seleccionado y bloqueado`">
                      <img :src="attributeRollDieImage(roll.damageD10)" alt="">
                      <strong>{{ roll.damageD10.value }}</strong>
                    </div>
                    <div class="weapon-aim-roll-damage-value" aria-live="polite">
                      <strong>{{ weaponAimRollDamage(roll).value }}</strong>
                      <span class="weapon-aim-roll-damage-tag">{{ weaponAimRollDamage(roll).label }}</span>
                    </div>
                  </div>
                </section>
                <div class="attribute-roll-result" :class="{ critical: weaponAimRollIsCritical(roll), fumble: weaponAimRollIsFumble(roll), incomplete: !weaponAimRollIsValid(roll) }" aria-live="polite">
                  <strong v-if="weaponAimRollIsValid(roll)">{{ weaponAimRollResult(roll) }}</strong>
                  <strong v-else>Falta seleccionar {{ weaponAimRollMissingSelection(roll) }}</strong>
                </div>
              </div>
            </article>
          </div>
        </div>
        <footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeWeaponAimRolls">Cerrar</button></footer>
      </section>
    </div>
     <div v-if="showWeaponDetailModal && selectedWeapon" class="modal-backdrop" @click.self="closeWeaponDetail">
      <section class="modal-card weapon-detail-modal" role="dialog" aria-modal="true" aria-labelledby="weapon-detail-modal-title">
        <header class="modal-header"><div><p class="eyebrow accent">INVENTARIO · ARMA</p><h2 id="weapon-detail-modal-title">{{ selectedWeapon.name }}</h2><p class="modal-copy">{{ weaponTypes.find(type => type.value === selectedWeapon?.weaponType)?.label || selectedWeapon.weaponType }} · {{ weaponSizes.find(size => size.value === selectedWeapon?.size)?.label || selectedWeapon.size }}</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeWeaponDetail">×</button></header>
        <div class="modal-body weapon-detail-body">
            <div class="weapon-detail-hero"><div class="weapon-detail-image"><img v-if="weaponImage(selectedWeapon)" :src="weaponImage(selectedWeapon)" :alt="selectedWeapon.name"><span v-else>⚔</span></div><dl class="weapon-detail-stats"><div><dt>Daño</dt><dd>{{ weaponDamage(selectedWeapon) }}</dd></div><div><dt>Puntería</dt><dd>{{ selectedWeapon.aim ?? '—' }}</dd></div><div><dt>Alcance</dt><dd>{{ selectedWeapon.range }}</dd></div><div><dt>Recarga</dt><dd>{{ selectedWeapon.reload }}</dd></div><div><dt>Cadencia</dt><dd>{{ weaponRate(selectedWeapon.rate) }}</dd></div><div><dt>Capacidad</dt><dd>{{ selectedWeapon.capacity }}</dd></div><div><dt>Balas cargadas</dt><dd>{{ selectedWeapon.loadedBullets }}/{{ selectedWeapon.capacity }}</dd></div><div><dt>Calibre</dt><dd>{{ selectedWeapon.caliber || '—' }}</dd></div><div><dt>Balas disponibles</dt><dd>{{ ammunitionForCaliber(selectedWeapon.caliber) }}</dd></div><div><dt>Fuego automático</dt><dd>{{ selectedWeapon.automaticFire || '—' }}</dd></div></dl></div>
           <p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p>
          <p v-if="selectedWeapon.extraRule" class="weapon-detail-rule"><strong>Regla extra:</strong> {{ selectedWeapon.extraRule }}</p>
          <section class="weapon-slot-actions"><h3>Ubicación</h3><p class="sheet-muted">Solo se muestran movimientos válidos: ambas armas deben poder entrar en el hueco de la otra.</p><div class="weapon-target-grid"><button v-for="target in weaponMoveTargets(selectedWeapon).filter(slot => slot !== selectedWeapon?.slot)" :key="target" class="button button-quiet" type="button" :disabled="weaponMoving" @click="moveWeapon(selectedWeapon, target)"><span>{{ weaponAt(target) ? 'Intercambiar' : 'Mover a hueco vacío' }}</span><small>{{ weaponSlots.find(slot => slot.value === target)?.label }}</small><em v-if="weaponAt(target)">{{ weaponAt(target)?.name }}</em></button></div></section>
        </div>
        <footer class="modal-actions"><button class="button button-primary" type="button" @click="editSelectedWeapon">Modificar</button><button class="button button-quiet" type="button" @click="closeWeaponDetail">Cerrar</button><button class="button button-danger" type="button" :disabled="weaponDeleting" @click="deleteWeapon">{{ weaponDeleting ? 'Borrando…' : 'Borrar arma' }}</button></footer>
      </section>
    </div>

    <div v-if="showCatalogWeaponModal && selectedCatalogWeapon" class="modal-backdrop" @click.self="showCatalogWeaponModal=false">
      <section class="modal-card weapon-detail-modal" role="dialog" aria-modal="true" aria-labelledby="catalog-weapon-detail-title">
        <header class="modal-header"><div><p class="eyebrow accent">AÑADIR ARMA</p><h2 id="catalog-weapon-detail-title">{{ selectedCatalogWeapon.name }}</h2><p class="modal-copy">{{ weaponTypes.find(type => type.value === selectedCatalogWeapon?.weaponType)?.label || selectedCatalogWeapon.weaponType }} · {{ weaponSizes.find(size => size.value === selectedCatalogWeapon?.size)?.label || selectedCatalogWeapon.size }}</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="showCatalogWeaponModal=false">×</button></header>
         <div class="modal-body weapon-detail-body"><div class="weapon-detail-hero"><div class="weapon-detail-image"><img v-if="weaponCatalogImage(selectedCatalogWeapon)" :src="weaponCatalogImage(selectedCatalogWeapon)" :alt="selectedCatalogWeapon.name"><span v-else>⚔</span></div><dl class="weapon-detail-stats"><div><dt>Daño</dt><dd>{{ weaponDamage(selectedCatalogWeapon) }}</dd></div><div><dt>Puntería</dt><dd>{{ selectedCatalogWeapon.aim ?? '—' }}</dd></div><div><dt>Alcance</dt><dd>{{ selectedCatalogWeapon.range }}</dd></div><div><dt>Recarga</dt><dd>{{ selectedCatalogWeapon.reload }}</dd></div><div><dt>Cadencia</dt><dd>{{ weaponRate(selectedCatalogWeapon.rate) }}</dd></div><div><dt>Capacidad</dt><dd>{{ selectedCatalogWeapon.capacity }}</dd></div><div><dt>Balas cargadas</dt><dd>{{ selectedCatalogWeapon.loadedBullets }}/{{ selectedCatalogWeapon.capacity }}</dd></div><div><dt>Calibre</dt><dd>{{ selectedCatalogWeapon.caliber || '—' }}</dd></div><div><dt>Fuego automático</dt><dd>{{ selectedCatalogWeapon.automaticFire || '—' }}</dd></div></dl></div><p v-if="selectedCatalogWeapon.extraRule" class="weapon-detail-rule"><strong>Regla extra:</strong> {{ selectedCatalogWeapon.extraRule }}</p><p v-if="inventoryError" class="error-banner" role="alert">{{ inventoryError }}</p></div>
        <footer class="modal-actions"><button class="button button-primary" type="button" :disabled="catalogLoading" @click="addSelectedCatalogWeapon">{{ catalogLoading ? 'Añadiendo…' : 'Añadir' }}</button><button class="button button-quiet" type="button" @click="showCatalogWeaponModal=false">Cancelar</button></footer>
      </section>
    </div>

    <div v-if="showAttributeRoll && attributeRoll" class="modal-backdrop attribute-roll-backdrop" @click.self="closeAttributeRoll">
      <section class="modal-card attribute-roll-modal" role="dialog" aria-modal="true" aria-labelledby="attribute-roll-title">
        <header class="modal-header">
          <div>
            <p class="eyebrow accent">{{ attributeRoll.abilityName ? `TIRADA · ${attributeRoll.abilityName}` : 'TIRADA DE ATRIBUTO' }}</p>
            <h2 id="attribute-roll-title">{{ attributeRoll.name }}</h2>
            <p class="modal-copy">{{ attributeRoll.score }} · +{{ attributeRoll.plusOne }} · +{{ attributeRoll.plusD6 }}D6</p>
          </div>
          <button id="attribute-roll-close" class="modal-close" type="button" aria-label="Cerrar tirada" @click="closeAttributeRoll">×</button>
        </header>
        <div class="modal-body attribute-roll-body">
          <div class="attribute-roll-layout">
            <section class="attribute-roll-dice-group attribute-roll-d10-group" aria-labelledby="attribute-roll-d10-label">
              <h3 id="attribute-roll-d10-label">D10</h3>
              <div class="attribute-roll-dice d10-dice">
                <button v-for="die in attributeRollD10Dice" :key="die.id" class="roll-die" :class="{ selected: die.selected }" type="button" :aria-pressed="die.selected" :aria-label="`D10: ${die.value}${die.selected ? ', seleccionado' : ', no seleccionado'}`" @click="toggleAttributeRollDie(die.id)">
                  <img :src="attributeRollDieImage(die)" alt="">
                  <strong>{{ die.value }}</strong>
                </button>
              </div>
            </section>
            <section class="attribute-roll-dice-group attribute-roll-d6-group" aria-labelledby="attribute-roll-d6-label">
              <h3 id="attribute-roll-d6-label">D6 disponibles</h3>
              <div class="attribute-roll-dice d6-dice">
                <button v-for="die in attributeRollD6Dice" :key="die.id" class="roll-die" :class="{ selected: die.selected, disabled: die.disabled }" type="button" :disabled="die.disabled" :aria-pressed="die.selected" :aria-label="`D6: ${die.value}${die.disabled ? ', desactivado por sacar 1' : die.selected ? ', seleccionado' : ', no seleccionado'}`" @click="toggleAttributeRollDie(die.id)">
                  <img :src="attributeRollDieImage(die)" alt="">
                  <strong>{{ die.value }}</strong>
                </button>
              </div>
            </section>
          <div class="attribute-roll-result" :class="{ critical: attributeRollIsCritical, fumble: attributeRollIsFumble, success: abilityRollSuccess, failure: attributeRoll.abilityName && attributeRollHasValidSelection && !abilityRollSuccess, incomplete: !attributeRollHasValidSelection }" aria-live="polite">
               <strong v-if="attributeRollHasValidSelection">{{ attributeRollResult }}<small v-if="attributeRoll.abilityName"> · dif {{ attributeRoll.difficulty }}+</small></strong>
              <strong v-else>Falta seleccionar {{ attributeRollMissingSelection }}</strong>
            </div>
          </div>
        </div>
        <footer class="modal-actions">
          <button class="button button-primary" type="button" @click="rerollAttribute">Volver a tirar</button>
          <button class="button button-quiet" type="button" @click="closeAttributeRoll">Cerrar</button>
        </footer>
      </section>
    </div>

  </main>

</template>

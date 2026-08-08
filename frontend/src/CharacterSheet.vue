<script setup lang="ts">

import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { useRoute, useRouter } from 'vue-router';

import { api, type AllocationPayload } from './services/api';



type Character = {

  id: string; name: string; imageUrl?: string | null; campaignId?: string | null;

  experience: number; level: number; attributes?: Record<string, number>; attributeTotals?: Record<string, number>; attributeModifiers?: Record<string, AttributeModifier[]>; derivedStats?: Record<string, DerivedStat>; genetics?: Record<string, number>; genetic?: Record<string, number>; allocation?: AllocationBudget;

  updatedAt?: string; createdAt?: string; closed?: boolean; lastClosedAt?: string; minorAttributes?: MinorAttribute[];
  abilities?: string[]; pendingUniqueAbilities?: string[];

};

type Campaign = { id: string; name: string };
type MinorAttribute = { id:string; key:string; name:string; value:number; ranks:number; total:number; max:number; maxFormula?:string; bonusSource?:string; plusOne:number; plusD6:number; type:string };
type AllocationBudget = { evolutionAvailable:number; evolutionSpent:number; evolutionRemaining:number; geneticsAvailable:number; geneticsSpent:number; geneticsRemaining:number; nextEvolutionReward?:number; nextGeneticsReward?:number; minorEvolutionCost?:number };
type AllocationDraft = { level:number; experience:number; evolutionAvailable:number; geneticsAvailable:number; minorEvolutionCost:number; attributes:Record<string,number>; genetics:Record<string,number>; minorAttributes:Record<string,number>; baseAttributes:Record<string,number>; baseGenetics:Record<string,number>; baseMinorAttributes:Record<string,number> };
type AttributeModifier = { name: string; value: number };
type DerivedStat = { key: string; name: string; formula: string; baseValue: number; total: number; modifiers?: AttributeModifier[] };
type Progression = { kind: string; number: number; threshold: number; obtained: boolean };
type AttributeDetail = { key:string; definitionId?:string | null; name:string; type:string; total:number; ranks:number; maxRanks:number | null; formula:string; calculatedValue:number; plusOne:number; plusD6:number; modifiers:AttributeModifier[]; progressions:Progression[]; deletable:boolean };
type AttributeRow = { key:string; name:string; value:number; definitionId?:string; deletable?:boolean };
type Ability = { name:string; description?:string; launchType?:string; cost?:number | string | null; test?:string; alternativesJson?:string; uniqueFlag?:string };
type PendingUniqueAbility = Ability & { requirements: unknown };
type LastUpgrade = { available:boolean; current?:{level:number; closedAt:string}; previous?:{level:number; closedAt:string}; scores?:{key:string; type:string; before:number; after:number; increase:number}[]; bonuses?:{key:string; plusOne:number; plusD6:number}[]; modifiers?:{key:string; name:string; before:number|null; after:number|null}[]; abilities?:string[] };
type HistoryVersion = { id:string; level:number; experience:number; createdAt:string; snapshot: Record<string, any> };



const route = useRoute();

const router = useRouter();

const character = ref<Character | null>(null);
const editing = ref(false);

const campaign = ref<Campaign | null>(null);

const loading = ref(true);

const error = ref('');

const attributeDetail = ref<AttributeDetail | null>(null);
const detailLoading = ref(false);
const detailError = ref('');
const showAttributeDetail = ref(false);
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
const sheetView = ref<'sheet' | 'abilities'>('sheet');
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
const isDirector = ref(localStorage.getItem('dexm.director') === 'true');
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

const majorAttributes = computed(() => majorKeys.map(key => [key, attributes.value[key] ?? 0] as [string, number]));

const minorAttributes = computed(() => { const built=minorKeys.filter(k=>k!=='astronavegar').map(key => [key, attributes.value[key] ?? 0] as [string, number]); const custom=(character.value?.minorAttributes??[]).map(a=>[a.key,a.value] as [string,number]); return [...built,...custom]; });
const derivedStatKeys = ['vida', 'bifrost', 'defensaCuerpo', 'defensaDistancia'];
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

const savedAt = computed(() => character.value?.lastClosedAt ? new Date(character.value.lastClosedAt).toLocaleString('es-ES') : 'Sin guardar');



async function load() {

  loading.value = true; error.value = '';

  try {

    character.value = await api.get(String(route.params.id));
    legacyDraft.value = false; legacyEvolutionPoints.value = null;
    editing.value = !character.value?.closed;
    if (editing.value) startModifierDraft();

    if (character.value?.campaignId) campaign.value = await api.campaign(character.value.campaignId);

  } catch (e: any) { error.value = e?.message || 'No se pudo cargar el personaje.'; }

  finally { loading.value = false; }

}

const showMinorModal=ref(false); const minorKind=ref('GALDR'); const customName=ref(''); const customFormula=ref(''); const customSource=ref('informatica'); const minorBusy=ref(false); const minorError=ref('');

function insertToken(t:string){ customFormula.value += t; }

function clearFormula(){customFormula.value='';}

async function addMinor(){ if(!campaign.value||!customName.value&&minorKind.value==='CUSTOM') return; minorBusy.value=true; minorError.value=''; try { const presets:any={GALDR:{key:'galdr',name:'Galdr',maxFormula:'min(cruzarbifrost,einherjer,sentiryggdrasil)',bonusSource:null,type:'GALDR'},ASTRONAVEGAR:{key:'astronavegar',name:'Astronavegar',maxFormula:'conduccion',bonusSource:'conduccion',type:'PRESET'},FORJA:{key:'forja',name:'Forja',maxFormula:'(fisico+estudio)/2',bonusSource:'informatica',type:'PRESET'}}; const body=minorKind.value==='CUSTOM'?{name:customName.value,maxFormula:customFormula.value,bonusSource:customSource.value,type:'CUSTOM'}:presets[minorKind.value]; await api.createMinorAttribute(campaign.value.id,body); character.value=await api.get(String(route.params.id)); showMinorModal.value=false; } catch(e:any){minorError.value=e?.message||'No se pudo crear';} finally{minorBusy.value=false;} }

function startModifierDraft() {
  if (!character.value) return;
  modifierDraft.value = Object.fromEntries(Object.entries(character.value.attributeModifiers ?? {}).map(([key, values]) => [key, values.map(value => ({ ...value }))]));
}

async function startEdit() {
  if (!character.value || editing.value) return;
  try {
    character.value = await api.edit(String(route.params.id));
    legacyDraft.value = false; legacyEvolutionPoints.value = null;
    editing.value = true;
    startModifierDraft();
    closeError.value = '';
  } catch (e: any) { closeError.value = e?.message || 'No se pudo abrir la ficha para edición.'; }
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

function detailModifierTotal(): number {
  if (!attributeDetail.value) return 0;
  const persistedTotal = attributeDetail.value.modifiers.reduce((sum, modifier) => sum + modifier.value, 0);
  if (!editing.value) return persistedTotal;
  return modifierRows(attributeDetail.value.key).reduce((sum, modifier) => sum + (Number(modifier.value) || 0), 0);
}

function detailTotal(): number {
  if (!attributeDetail.value) return 0;
  if (attributeDetail.value.type === 'DERIVED') return derivedBaseValue(attributeDetail.value.key) + detailModifierTotal();
  return attributeDetail.value.total - attributeDetail.value.modifiers.reduce((sum, modifier) => sum + modifier.value, 0) + detailModifierTotal();
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
  const draftModifiers = modifierDraft.value[key] ?? persistedModifiers;
  const persistedModifierTotal = persistedModifiers.reduce((sum, modifier) => sum + modifier.value, 0);
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
    });
    character.value = result.character;
    editing.value = false;
    legacyDraft.value = false; legacyEvolutionPoints.value = null;
  } catch (e: any) { closeError.value = e?.message || 'No se pudo cerrar la ficha.'; }
  finally { closeBusy.value = false; }
}

async function cancelChanges() {
  if (!editing.value || cancelChangesBusy.value || !confirm('¿Cancelar todos los cambios y volver a la última versión cerrada?')) return;
  cancelChangesBusy.value = true; closeError.value = '';
  try {
    const result = await api.cancelChanges(String(route.params.id)) as { character: Character };
    character.value = result.character; editing.value = false; legacyDraft.value = false; legacyEvolutionPoints.value = null; startModifierDraft();
  } catch (e: any) { closeError.value = e?.message || 'No se pudieron cancelar los cambios.'; }
  finally { cancelChangesBusy.value = false; }
}

async function openHistory() {
  historyLoading.value = true; historyError.value = '';
  try { history.value = await api.milestones(String(route.params.id)) as HistoryVersion[]; selectedHistory.value = history.value[0] ?? null; showHistory.value = true; }
  catch (e: any) { historyError.value = e?.message || 'No se pudo cargar el historial.'; }
  finally { historyLoading.value = false; }
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

const abilityActivationTest = computed(() => {
  const rawTest = abilityTestValue(selectedAbility.value)?.trim();
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
    return { rawTest, testName, difficulty: match?.[2] === '*' ? '*' : match?.[2] ? `${match[2]}+` : null, score: null, plusOne: null, plusD6: null };
  }

  const custom = customMinor(key);
  const score = displayedAttributeTotal(key, custom?.total ?? attributes.value[key] ?? 0);
  return {
    rawTest,
    testName,
    difficulty: match?.[2] === '*' ? '*' : match?.[2] ? `${match[2]}+` : null,
    score,
    plusOne: custom?.plusOne ?? oneBonus(key, score, majorKeys.includes(key)),
    plusD6: custom?.plusD6 ?? d6Bonus(key, score, majorKeys.includes(key)),
  };
});

async function loadAbilities() {
  abilityCatalogLoading.value = true; abilityCatalogError.value = '';
  try { abilityCatalog.value = await api.abilities() as Ability[]; }
  catch (e: any) { abilityCatalogError.value = e?.message || 'No se pudo cargar el catálogo de habilidades.'; }
  finally { abilityCatalogLoading.value = false; }
}

function openAbilityDetail(ability: Ability) { selectedAbility.value = ability; }
function closeAbilityDetail() { selectedAbility.value = null; }
function onEscape(event: KeyboardEvent) { if (event.key === 'Escape') closeAbilityDetail(); }

onMounted(() => { load(); loadAbilities(); window.addEventListener('keydown', onEscape); });
onBeforeUnmount(() => window.removeEventListener('keydown', onEscape));

watch(() => route.params.id, (id, previousId) => { if (id && id !== previousId) load(); });

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
        <p v-if="closeError" class="error-banner" role="alert">{{ closeError }}</p>

        <template v-if="editing"><button class="button button-primary" :disabled="closeBusy || levelBusy || modifierSaveBusy" @click="closeDraft">{{ closeBusy ? 'Guardando…' : 'Guardar' }}</button><button class="button button-quiet" type="button" :disabled="cancelChangesBusy" @click="cancelChanges">{{ cancelChangesBusy ? 'Cancelando…' : 'Cancelar cambios' }}</button><button class="button button-quiet" type="button" @click="showLegacyImport=true; legacyError=''">Cargar legacy</button><button class="button button-quiet" type="button" @click="openCurrentUpgrade">Ver subida actual</button></template><template v-else><button class="button button-quiet" type="button" @click="startEdit">Editar ficha</button><button class="button button-quiet" type="button" @click="exportLegacy">Exportar legacy</button><button class="button button-quiet" type="button" @click="openLastUpgrade">Última subida</button></template><button class="button button-quiet" type="button" @click="openHistory">Historial</button><button v-if="isDirector && (character.pendingUniqueAbilities?.length ?? 0)" class="button button-primary unique-review-trigger" type="button" @click="openUniqueReview">Revisar habilidades únicas</button><button class="button button-quiet" type="button" @click="sheetView = sheetView === 'sheet' ? 'abilities' : 'sheet'">{{ sheetView === 'sheet' ? 'Ver habilidades' : 'Ver hoja' }}</button><button class="button button-quiet" type="button" @click="back">Volver</button><button v-if="isDirector" class="button button-danger" type="button" @click="deleteCharacter">Borrar personaje</button>
        <p v-if="editing" class="field-hint">Haz clic en un atributo para editar sus modificadores varios.</p><p v-if="modifierError" class="error-banner" role="alert">{{ modifierError }}</p>

      </aside>

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
            <section class="detail-section"><h3>Modificadores varios</h3><ul v-if="(editing ? modifierRows(attributeDetail.key) : attributeDetail.modifiers).length" class="modifier-list"><li v-for="modifier in (editing ? modifierRows(attributeDetail.key) : attributeDetail.modifiers)" :key="modifier.name"><span>{{ modifier.name }}</span><strong>{{ modifier.value > 0 ? '+' : '' }}{{ modifier.value }}</strong></li></ul><p v-else class="sheet-muted">Sin modificadores varios.</p></section><section v-if="editing" class="detail-section modifier-editor"><h3>Editar modificadores</h3><div v-for="(modifier, index) in modifierRows(attributeDetail.key)" :key="index" class="modifier-edit-row"><input v-model="modifier.name" aria-label="Nombre del modificador" placeholder="Nombre"><input v-model.number="modifier.value" type="number" step="1" aria-label="Valor del modificador"><button class="button button-danger" type="button" @click="removeModifier(attributeDetail.key, index)">Eliminar</button></div><button class="button button-quiet" type="button" @click="addModifier(attributeDetail.key)">Añadir modificador</button></section>
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
          <header class="modal-header"><div><p class="eyebrow accent">HABILIDAD OBTENIDA</p><h2 id="ability-detail-title">{{ selectedAbility.name }}</h2></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeAbilityDetail">×</button></header>
          <div class="modal-body ability-detail-body">
            <p class="ability-description">{{ selectedAbility.description || 'Sin descripción disponible.' }}</p>
            <dl class="ability-meta"><div><dt>Lanzamiento</dt><dd>{{ selectedAbility.launchType || '—' }}</dd></div><div><dt>Coste</dt><dd>{{ selectedAbility.cost ?? '—' }}</dd></div><div><dt>Prueba de activación</dt><dd v-if="abilityActivationTest">{{ abilityActivationTest.difficulty ? abilityActivationTest.testName + ' ' + abilityActivationTest.difficulty : abilityActivationTest.rawTest }}<template v-if="abilityActivationTest.score !== null"> (+{{ abilityActivationTest.plusOne }} +{{ abilityActivationTest.plusD6 }}D6)</template></dd><dd v-else>—</dd></div></dl>
          </div>
          <footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeAbilityDetail">Cerrar</button></footer>
        </section>
      </div>

<div v-if="showHistory" class="modal-backdrop" @click.self="showHistory=false"><section class="modal-card history-modal" role="dialog" aria-modal="true" aria-labelledby="history-title"><header class="modal-header"><div><p class="eyebrow accent">FICHA · HISTORIAL</p><h2 id="history-title">Versiones cerradas</h2><p class="modal-copy">Recorre las versiones guardadas y recupera una como nueva versión actual.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="showHistory=false">×</button></header><div class="modal-body"><p v-if="historyLoading" class="sheet-state">Cargando historial…</p><p v-else-if="historyError" class="error-banner" role="alert">{{ historyError }}</p><p v-else-if="!history.length" class="sheet-state">Aún no hay versiones cerradas.</p><ol v-else class="history-list"><li v-for="version in history" :key="version.id" class="history-item"><button class="history-select" type="button" @click="selectedHistory=version"><div><strong>Nivel {{ version.level }}</strong><span>{{ new Date(version.createdAt).toLocaleString('es-ES') }}</span><small>{{ version.experience }} PX · {{ Object.keys(version.snapshot?.attributes || {}).length }} atributos</small></div></button><button class="button button-primary" type="button" :disabled="historyRecovering === version.id" @click.stop="recoverHistory(version)">{{ historyRecovering === version.id ? 'Recuperando…' : 'Recuperar' }}</button></li></ol><section v-if="selectedHistory" class="history-detail"><h3>Versión seleccionada</h3><p><strong>{{ selectedHistory.snapshot?.name || character.name }}</strong> · Nivel {{ selectedHistory.level }} · {{ selectedHistory.experience }} PX</p><p class="sheet-muted">Evolución: {{ selectedHistory.snapshot?.evolutionPoints ?? 'no disponible' }} · Genética: {{ selectedHistory.snapshot?.geneticsPoints ?? 'no disponible' }}</p><p class="sheet-muted">Habilidades: {{ (selectedHistory.snapshot?.abilities || []).length }} · Modificadores: {{ Object.values(selectedHistory.snapshot?.modifiers || {}).flat().length }}</p></section></div><footer class="modal-actions"><button class="button button-quiet" type="button" @click="showHistory=false">Cerrar</button></footer></section></div>

      <div v-if="showLastUpgrade" class="modal-backdrop" @click.self="closeLastUpgrade"><section class="modal-card last-upgrade-modal" role="dialog" aria-modal="true" aria-labelledby="last-upgrade-title"><header class="modal-header"><div><p class="eyebrow accent">PROGRESIÓN</p><h2 id="last-upgrade-title">{{ currentUpgradeMode ? 'Ver subida actual' : 'Última subida' }}</h2><p v-if="lastUpgrade?.available" class="modal-copy">Comparación entre la versión actual y el nivel {{ lastUpgrade.previous?.level }}.</p></div><button class="modal-close" type="button" aria-label="Cerrar ventana" @click="closeLastUpgrade">×</button></header><div class="modal-body"><p v-if="lastUpgradeLoading" class="sheet-state">{{ currentUpgradeMode ? 'Comparando subida actual…' : 'Comparando versiones cerradas…' }}</p><p v-else-if="lastUpgradeError" class="error-banner" role="alert">{{ lastUpgradeError }}</p><p v-else-if="!lastUpgrade?.available" class="sheet-state">Aún no existe una versión cerrada anterior para comparar.</p><template v-else><section class="upgrade-section"><h3>Nuevas puntuaciones</h3><ul v-if="lastUpgrade.scores?.length" class="upgrade-list"><li v-for="change in lastUpgrade.scores" :key="`${change.type}-${change.key}`"><span>{{ upgradeScoreLabel(change) }}</span><strong>{{ change.before }} → {{ change.after }} <em>+{{ change.increase }}</em></strong></li></ul><p v-else class="sheet-muted">Sin nuevas puntuaciones.</p></section><section class="upgrade-section"><h3>Modificadores varios</h3><ul v-if="lastUpgrade.modifiers?.length" class="upgrade-list"><li v-for="change in lastUpgrade.modifiers" :key="`${change.key}-${change.name}`"><span>{{ attributeLabels[change.key] || change.key }} · {{ change.name }}</span><strong>{{ change.before ?? '—' }} → {{ change.after ?? '—' }}</strong></li></ul><p v-else class="sheet-muted">Sin cambios en modificadores varios.</p></section><section class="upgrade-section"><h3>Nuevos bonificadores</h3><ul v-if="lastUpgrade.bonuses?.length" class="upgrade-list"><li v-for="bonus in lastUpgrade.bonuses" :key="bonus.key"><span>{{ attributeLabels[bonus.key] || bonus.key }}</span><strong><template v-if="bonus.plusOne">+{{ bonus.plusOne }}</template><template v-if="bonus.plusOne && bonus.plusD6"> · </template><template v-if="bonus.plusD6">+{{ bonus.plusD6 }}D6</template></strong></li></ul><p v-else class="sheet-muted">Sin nuevos +1 ni +D6.</p></section><section class="upgrade-section"><h3>Nuevas habilidades</h3><ul v-if="lastUpgradeAbilities.length" class="upgrade-list"><li v-for="ability in lastUpgradeAbilities" :key="ability.name"><button class="upgrade-ability-link" type="button" @click="openAbilityDetail(ability)">{{ ability.name }}</button></li></ul><p v-else class="sheet-muted">No se han obtenido habilidades nuevas.</p></section></template></div><footer class="modal-actions"><button class="button button-quiet" type="button" @click="closeLastUpgrade">Cerrar</button></footer></section></div>

      <section v-if="sheetView === 'sheet'" class="sheet-content" aria-labelledby="sheet-title">

<div class="sheet-heading"><div><p class="eyebrow accent">HOJA DE PERSONAJE</p><h2 id="sheet-title">{{ character.name }}</h2><p v-if="levelError" class="error-banner" role="alert">{{ levelError }}</p></div><div class="sheet-level"><span>Nivel</span><strong>{{ level }}</strong><small @dblclick="editing && (showExperienceModal=true, experienceError='')" title="Doble clic para añadir experiencia">{{ character.experience }} PX</small><div v-if="editing" class="level-actions"><button class="button button-quiet" type="button" :disabled="!canLevelUp || levelBusy" @click="openLevelUp()">Subir nivel</button><button class="button button-quiet" type="button" :disabled="!canLevelUpAll || levelBusy" @click="openLevelUp(true)">Subir varios niveles</button><button class="button button-quiet" type="button" @click="showExperienceModal=true; experienceError=''">Añadir experiencia</button></div></div></div>

        <section class="sheet-panel"><h3>Atributos Mayores</h3><div class="value-grid attributes-grid major-attributes-grid"><div v-for="[key, value] in majorAttributes" :key="key" class="value-row attribute-row attribute-clickable" role="button" tabindex="0" @click="openAttributeDetail(key)" @keydown.enter="openAttributeDetail(key)" @keydown.space.prevent="openAttributeDetail(key)"><span>{{ attributeLabels[key] || key }}</span><strong>{{ displayedAttributeTotal(key, value) }}</strong><small>+{{ oneBonus(key, displayedAttributeTotal(key, value), true) }} · +{{ d6Bonus(key, displayedAttributeTotal(key, value), true) }}D6</small></div><p v-if="!Object.keys(attributes).length" class="sheet-muted">Sin atributos registrados.</p></div></section>

        <section class="sheet-panel"><div class="sheet-panel-heading"><h3>Atributos Menores</h3><button v-if="editing" class="button button-quiet" type="button" @click="showMinorModal=true">Añadir atributo menor</button></div><div class="value-grid attributes-grid"><div v-for="[key, value] in minorAttributes" :key="key" class="value-row attribute-row attribute-clickable" role="button" tabindex="0" @click="openAttributeDetail(key)" @keydown.enter="openAttributeDetail(key)" @keydown.space.prevent="openAttributeDetail(key)"><span>{{ attributeLabels[key] || customMinor(key)?.name || key }}</span><strong>{{ displayedAttributeTotal(key, customMinor(key)?.total ?? value) }}</strong><small>+{{ maxBonus(key, displayedAttributeTotal(key, customMinor(key)?.total ?? value), false) }} · +{{ maxD6(key, displayedAttributeTotal(key, customMinor(key)?.total ?? value), false) }}D6</small></div></div></section>

        <section class="sheet-panel"><h3>Genética</h3><div class="genetic-groups"><div v-for="group in geneticGroups" :key="group.label" class="genetic-group"><h4>{{ group.label }}</h4><div class="value-grid genetic-grid"><div v-for="key in group.keys" :key="key" class="value-row attribute-clickable" role="button" tabindex="0" @click="openAttributeDetail(key)" @keydown.enter="openAttributeDetail(key)" @keydown.space.prevent="openAttributeDetail(key)"><span>{{ geneticLabels[key] || key }}</span><strong>{{ geneticTotals[key] ?? 0 }}</strong></div></div></div></div><p v-if="!Object.keys(genetics).length" class="sheet-muted">Sin valores genéticos.</p></section>

        <section class="sheet-panel"><h3>Valores calculados</h3><div class="value-grid attributes-grid derived-stats-grid"><div v-for="key in derivedStatKeys" :key="key" class="value-row attribute-row attribute-clickable" role="button" tabindex="0" @click="openAttributeDetail(key)" @keydown.enter="openAttributeDetail(key)" @keydown.space.prevent="openAttributeDetail(key)"><span>{{ derivedStats[key]?.name || key }}</span><strong>{{ displayedAttributeTotal(key, derivedStats[key]?.total ?? 0) }}</strong><small>{{ derivedStats[key]?.formula || '—' }}</small></div></div></section>

      </section>

      <section v-else class="sheet-content abilities-content" aria-labelledby="abilities-title">
        <div class="sheet-heading"><div><p class="eyebrow accent">HABILIDADES</p><h2 id="abilities-title">{{ character.name }}</h2><p class="modal-copy">Todas las habilidades obtenidas en la última versión cerrada.</p></div><strong class="ability-count">{{ obtainedAbilities.length }}</strong></div>
        <p v-if="abilityCatalogLoading" class="sheet-state">Cargando habilidades…</p>
        <p v-else-if="abilityCatalogError" class="error-banner" role="alert">{{ abilityCatalogError }}</p>
        <div v-else-if="obtainedAbilities.length" class="abilities-grid">
          <button v-for="ability in obtainedAbilities" :key="ability.name" class="ability-card" type="button" @click="openAbilityDetail(ability)">{{ ability.name }}</button>
        </div>
        <p v-else class="sheet-state">No hay habilidades obtenidas en una versión cerrada.</p>
      </section>

    </div>

  </main>

</template>

export type TrainingRuleActivity = { id?: string; type: string; name: string; startAge: number; endAge: number; priority: number; concurrent: boolean; primaryAttribute?: string | null; secondaryAttribute?: string | null; tertiaryAttribute?: string | null };
export type TrainingRuleCharacter = { einherjer?: boolean; einherjerOrigin?: string | null; awakeningAge?: number | null; attributes?: Record<string, number> };
export type TrainingRulePreview = { humanYears: number; modifiers: { attributeKey: string; baseValue: number; previousSelections: number; value: number }[] };

function humanEquivalent(activity: TrainingRuleActivity, character: TrainingRuleCharacter): number {
  if (activity.type.toUpperCase() === 'COURSE') return 0;
  let total = 0;
  for (let age = activity.startAge; age < activity.endAge; age += 1) {
    const speed = !character.einherjer || character.awakeningAge == null || age < character.awakeningAge ? 1 : character.einherjerOrigin === 'converted' ? 2 : 3;
    total += speed;
  }
  return activity.concurrent && activity.type.toUpperCase() === 'OCCUPATION' ? total / 1.5 : total;
}
function bonus(type: string, years: number): [number, number, number] {
  const y = Math.floor(years + 1e-9);
  if (type.toUpperCase() === 'COURSE') return [2, 0, 0];
  if (type.toUpperCase() === 'FORMATION') return y >= 8 ? [6,4,2] : y >= 6 ? [5,3,2] : y >= 4 ? [4,2,1] : y >= 2 ? [3,1,0] : y >= 1 ? [2,0,0] : [0,0,0];
  if (type.toUpperCase() === 'PROFESSION') return y >= 20 ? [6,4,3] : y >= 15 ? [5,3,2] : y >= 10 ? [4,2,1] : y >= 5 ? [3,1,0] : y >= 1 ? [1,0,0] : [0,0,0];
  return y >= 15 ? [4,3,2] : y >= 10 ? [3,2,2] : y >= 5 ? [3,1,1] : y >= 3 ? [2,1,0] : y >= 1 ? [1,0,0] : [0,0,0];
}
export function calculateTrainingPreview(character: TrainingRuleCharacter, activities: TrainingRuleActivity[], preview: TrainingRuleActivity, replacingId?: string | null): TrainingRulePreview {
  const ordered = [...activities.filter(item => item.id !== replacingId), { ...preview, id: replacingId ?? '__preview__' }].sort((a,b) => (a.type.toUpperCase() === 'COURSE' ? 1 : 0) - (b.type.toUpperCase() === 'COURSE' ? 1 : 0) || a.startAge - b.startAge || a.priority - b.priority);
  const selections = new Map<string, number>();
  const totals = new Map<string, number>();
  let result: TrainingRulePreview = { humanYears: 0, modifiers: [] };
  for (const activity of ordered) {
    const years = humanEquivalent(activity, character); const values = bonus(activity.type, years); const modifiers: TrainingRulePreview['modifiers'] = [];
    [activity.primaryAttribute, activity.secondaryAttribute, activity.tertiaryAttribute].forEach((key, index) => {
      if (!key) return;
      let baseValue: number; let previousSelections = 0; let value: number;
      if (activity.type.toUpperCase() === 'COURSE') { const current = totals.get(key) ?? 0; baseValue = current < 2 ? 2 : current < 5 ? 1 : 0; if ((character.attributes?.[key] ?? 0) > 0) baseValue = 0; value = baseValue; }
      else { baseValue = values[index]; previousSelections = selections.get(key) ?? 0; value = baseValue / 2 ** previousSelections; selections.set(key, previousSelections + 1); }
      totals.set(key, (totals.get(key) ?? 0) + value); modifiers.push({ attributeKey: key, baseValue, previousSelections, value });
    });
    if (activity.id === '__preview__' || activity.id === replacingId) result = { humanYears: years, modifiers };
  }
  return result;
}
export function normalizeAbilityKey(key: string): string { const normalized = key.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]/g, ''); return ({fis:'fisico',agi:'agilidad',pcn:'percepcion',mnt:'mente',est:'estudio',car:'carisma',enganno:'engano',evoluccioncurva:'evolcurva',evolutivocurva:'evolcurva'} as Record<string,string>)[normalized] ?? normalized; }
export function effectiveAbilityGenetics(genetics: Record<string, number>, modifiers: Record<string, Array<{ value?: number }>> | undefined): Record<string, number> {
  const result = { ...genetics };
  Object.entries(modifiers ?? {}).forEach(([key, values]) => {
    const total = values.reduce((sum, modifier) => sum + (Number(modifier.value) || 0), 0);
    if (total !== 0) result[key] = (Number(result[key]) || 0) + total;
  });
  return result;
}
export function abilityEligible(alternativesJson: string | undefined, values: Record<string, number>, genetics: Record<string, number>): boolean {
  let alternatives: unknown; try { alternatives = JSON.parse(alternativesJson || '[]'); } catch { return false; }
  if (!Array.isArray(alternatives)) return false; const ignored = new Set(['Nombre','Descripcion','Lanzamiento','Coste','Prueba','Unica']);
  return alternatives.some(item => !!item && typeof item === 'object' && Object.entries(item as Record<string,unknown>).filter(([key,value]) => !ignored.has(key) && typeof value === 'number').every(([key,value]) => (values[normalizeAbilityKey(key)] ?? genetics[normalizeAbilityKey(key)] ?? 0) >= Number(value)));
}

export type AbilityRuleCatalogEntry = { name: string; alternativesJson?: string; uniqueFlag?: string };
export type UniqueAbilityDecision = 'accepted' | 'rejected' | string;
export type AbilityAwards = { obtained: string[]; pendingUnique: string[] };

export function calculateAbilityAwards(
  catalog: AbilityRuleCatalogEntry[],
  values: Record<string, number>,
  genetics: Record<string, number>,
  decisions: Record<string, UniqueAbilityDecision> = {},
): AbilityAwards {
  const obtained: string[] = [];
  const pendingUnique: string[] = [];
  for (const ability of catalog) {
    if (!abilityEligible(ability.alternativesJson, values, genetics)) continue;
    const unique = ['si', 'sí', 'true'].includes(String(ability.uniqueFlag ?? '').trim().toLowerCase());
    if (!unique) {
      obtained.push(ability.name);
      continue;
    }
    const decision = decisions[ability.name];
    if (decision === 'accepted') obtained.push(ability.name);
    else if (decision !== 'rejected') pendingUnique.push(ability.name);
  }
  return { obtained, pendingUnique };
}

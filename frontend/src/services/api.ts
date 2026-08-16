import { ApiError, safePath, logClientError, apiErrorFromResponse } from './errors';

const csrfToken = () => document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=');
const request = async (path: string, options: RequestInit = {}) => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...(options.headers as Record<string,string> || {}) };
  const requestId = crypto.randomUUID(); headers['X-Request-Id'] = requestId;
  const token = csrfToken(); if (token && options.method && options.method !== 'GET') headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
  let response: Response;
  try { response = await fetch(path, { ...options, credentials: 'same-origin', headers }); }
  catch (cause) { const error = new ApiError('No se ha podido conectar con el servidor.', { method: options.method || 'GET', path: safePath(path), requestId }); logClientError(cause, { source: 'api', method: error.method, path: error.path, requestId }); throw error; }
  if (!response.ok) {
    const error = await apiErrorFromResponse(response, requestId, options.method || 'GET', path);
    logClientError(error, { source: 'api', status: error.status, code: error.code, requestId: error.requestId, method: error.method, path: error.path });
    throw error;
  }
  if (!path.endsWith('/keepalive')) window.dispatchEvent(new CustomEvent('dexm-api-activity'));
  if (response.status === 204) return null;
  return response.json();
};

export type AllocationPayload = {
  level: number;
  experience: number;
  attributes: Record<string, number>;
  genetics: Record<string, number>;
  minorAttributes: Record<string, number>;
  visible: boolean;
  final: boolean;
};

export type CreationConfigurationPayload = {
  mode: 'empty' | 'guided';
  race?: string | null;
  einherjer?: boolean | null;
  awakened?: boolean | null;
  einherjerOrigin?: 'converted' | 'born_human' | 'born_einherjer' | null;
  startingAge?: number | null;
  awakeningAge?: number | null;
  sheetAge?: number | null;
  selectedMajorAttributes: string[];
  wizardState: 'empty' | 'started' | 'race' | 'majors' | 'einherjer' | 'complete';
};

export type CharacterSummary = {
  id: string;
  name: string;
  imageUrl?: string | null;
  campaignId?: string | null;
  closed?: boolean;
  editorEmails?: string[];
  canEdit: boolean;
};

export type OtherInventoryItem = {
  id: string;
  name: string;
  description?: string | null;
  location?: string | null;
  quantity: number;
  unitValue?: number | null;
};
export type AmmunitionType = 'CALIBER' | 'GRENADE';
export type GrenadeCatalogItem = {
  id: string; name: string; description?: string | null;
  centralDamage: number; adjacentDamage: number; damageDecay: number;
  additionalEffect?: string | null;
  handGrenade: boolean; type?: string | null; official: boolean;
};
export type Ammunition = { id: string; type: AmmunitionType; caliber?: string | null; grenadeCatalogId?: string | null; grenade?: GrenadeCatalogItem | null; quantity: number };
export type WeaponReloadResult = {
  weaponId: string;
  caliber: string;
  requested: number;
  consumed: number;
  remaining: number;
  missing: number;
  complete: boolean;
  loadedBullets: number;
};
export type WeaponShootResult = {
  weaponId: string;
  mode: string;
  requested: number;
  consumed: number;
  remaining: number;
  cadence: number;
  automaticFire?: number | null;
  loadedBullets: number;
};

export type Weapon = {
  id: string; slot: string; name: string; weaponType: string; size: string;
  range: number; reload: number; rate: string; damageVital: number; damageNormal: number;
  damageLight: number; damageVeryLight: number; aim?: number | null; automaticFire?: string | null;
  capacity: number; loadedBullets: number; caliber?: string | null; extraRule?: string | null; catalogWeaponId?: string | null; imageUrl?: string | null;
};
export type WeaponCatalogItem = Omit<Weapon, 'slot'> & { official: boolean };

export type ArmorSlot = 'HEAD' | 'BODY' | 'LEGS' | 'ARMS';
export type Armor = {
  id: string;
  name: string;
  description?: string | null;
  specialEffect?: string | null;
  defensePenalty: number;
  meleeDefensePenalty: number;
  rangedDefensePenalty: number;
  movementPenalty: number;
  zone?: string | null;
  slots: ArmorSlot[];
  rdBySlot: Record<ArmorSlot, number>;
  armorBySlot: Record<ArmorSlot, number>;
  catalogArmorId?: string | null;
  imageUrl?: string | null;
};
export type ArmorCatalogItem = Omit<Armor, 'id'> & { id: string; official: boolean };
export type Shield = {
  id: string;
  name: string;
  description?: string | null;
  hitPoints: number;
  catalogShieldId?: string | null;
  imageUrl?: string | null;
};
export type ShieldCatalogItem = Omit<Shield, 'id'> & { id: string; official: boolean };
export type PhysicalShield = { id: string; name: string; description?: string | null; rd: number; armor: number; defense: number; movement: number; size?: string | null; otherEffects?: string | null; imageUrl?: string | null; };
export type PhysicalShieldCatalogItem = Omit<PhysicalShield, 'id'> & { id: string; official: boolean };
export type CharacterWorkspace = {
  character: unknown;
  inventory: { others: OtherInventoryItem[]; weapons: Weapon[]; ammunition: Ammunition[]; armors: Armor[]; shields: Shield[]; physicalShields: PhysicalShield[] };
  training: unknown;
  abilities: { catalog: unknown[]; abilities: string[]; pendingUniqueAbilities: string[] };
};

export const api = {
  me: () => request('/api/auth/me'),
  keepalive: () => request('/api/auth/keepalive', { method: 'POST' }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  campaigns: () => request('/api/campaigns'),
  createCampaign: (name: string) => request('/api/campaigns', { method: 'POST', body: JSON.stringify({ name }) }),
  deleteCampaign: (id: string) => request('/api/campaigns/' + id, { method: 'DELETE' }),
  campaignMembers: (id: string) => request('/api/campaigns/' + id + '/members'),
  inviteCampaignMember: (id: string, email: string) => request('/api/campaigns/' + id + '/members', { method: 'POST', body: JSON.stringify({ email }) }),
  revokeCampaignMember: (id: string, email: string) => request('/api/campaigns/' + id + '/members/' + encodeURIComponent(email), { method: 'DELETE' }),
  campaign: (id: string) => request('/api/campaigns/' + id),
  exportCampaignArchive: (id: string) => request('/api/campaigns/' + encodeURIComponent(id) + '/archive'),
  importCampaignArchive: (id: string, archive: unknown) => request('/api/campaigns/' + encodeURIComponent(id) + '/archive/import', { method: 'POST', body: JSON.stringify(archive) }),
  minorAttributes: (id: string) => request('/api/campaigns/' + id + '/minor-attributes'),
  createMinorAttribute: (id: string, body: unknown) => request('/api/campaigns/' + id + '/minor-attributes', { method: 'POST', body: JSON.stringify(body) }),
  deleteMinorAttribute: (characterId: string, definitionId: string) => request('/api/characters/' + characterId + '/minor-attributes/' + definitionId, { method: 'DELETE' }),
characters: (campaignId: string) => request('/api/campaigns/' + campaignId + '/characters') as Promise<CharacterSummary[]>,
createCharacter: (campaignId: string, body: unknown) => request('/api/campaigns/' + campaignId + '/characters', { method: 'POST', body: JSON.stringify(body) }) as Promise<CharacterSummary>,
characterEditors: (id: string) => request('/api/characters/' + encodeURIComponent(id) + '/editors') as Promise<string[]>,
addCharacterEditor: (id: string, email: string) => request('/api/characters/' + encodeURIComponent(id) + '/editors', { method: 'POST', body: JSON.stringify({ email }) }) as Promise<string[]>,
removeCharacterEditor: (id: string, email: string) => request('/api/characters/' + encodeURIComponent(id) + '/editors/' + encodeURIComponent(email), { method: 'DELETE' }) as Promise<string[]>,
  exportCharacterArchive: (id: string) => request('/api/characters/' + encodeURIComponent(id) + '/archive'),
  importCharacterArchive: (id: string, archive: unknown) => request('/api/characters/' + encodeURIComponent(id) + '/archive/import', { method: 'POST', body: JSON.stringify(archive) }),
  configureCreation: (id: string, body: CreationConfigurationPayload) => request('/api/characters/' + id + '/creation', { method: 'POST', body: JSON.stringify(body) }),
  training: (id: string) => request('/api/characters/' + id + '/training'),
  inventory: (id: string) => request('/api/characters/' + id + '/inventory') as Promise<{ others: OtherInventoryItem[]; weapons: Weapon[]; ammunition: Ammunition[]; armors: Armor[]; shields: Shield[]; physicalShields: PhysicalShield[] }>,
  otherInventory: (id: string) => request('/api/characters/' + id + '/inventory/others') as Promise<OtherInventoryItem[]>,
  createOtherInventory: (id: string, body: Omit<OtherInventoryItem, 'id'>) => request('/api/characters/' + id + '/inventory/others', { method: 'POST', body: JSON.stringify(body) }) as Promise<OtherInventoryItem>,
  getOtherInventory: (id: string, itemId: string) => request('/api/characters/' + id + '/inventory/others/' + encodeURIComponent(itemId)) as Promise<OtherInventoryItem>,
  updateOtherInventory: (id: string, itemId: string, body: Omit<OtherInventoryItem, 'id'>) => request('/api/characters/' + id + '/inventory/others/' + encodeURIComponent(itemId), { method: 'PUT', body: JSON.stringify(body) }) as Promise<OtherInventoryItem>,
  deleteOtherInventory: (id: string, itemId: string) => request('/api/characters/' + id + '/inventory/others/' + encodeURIComponent(itemId), { method: 'DELETE' }),
  weapons: (id: string) => request('/api/characters/' + id + '/inventory/weapons') as Promise<Weapon[]>,
  createWeapon: (id: string, body: Omit<Weapon, 'id'>) => request('/api/characters/' + id + '/inventory/weapons', { method: 'POST', body: JSON.stringify(body) }) as Promise<Weapon>,
  updateWeapon: (id: string, weaponId: string, body: Omit<Weapon, 'id'>) => request('/api/characters/' + id + '/inventory/weapons/' + encodeURIComponent(weaponId), { method: 'PUT', body: JSON.stringify(body) }) as Promise<Weapon>,
  deleteWeapon: (id: string, weaponId: string) => request('/api/characters/' + id + '/inventory/weapons/' + encodeURIComponent(weaponId), { method: 'DELETE' }),
  moveWeapon: (id: string, weaponId: string, slot: string) => request('/api/characters/' + id + '/inventory/weapons/' + encodeURIComponent(weaponId) + '/move', { method: 'POST', body: JSON.stringify({ slot }) }) as Promise<Weapon>,
  reloadWeapon: (id: string, weaponId: string) => request('/api/characters/' + id + '/inventory/weapons/' + encodeURIComponent(weaponId) + '/reload', { method: 'POST' }) as Promise<WeaponReloadResult>,
  shootWeapon: (id: string, weaponId: string, shots: number, automatic = false) => request('/api/characters/' + id + '/inventory/weapons/' + encodeURIComponent(weaponId) + '/shoot', { method: 'POST', body: JSON.stringify({ mode: automatic ? 'automatic' : 'normal', shots }) }) as Promise<WeaponShootResult>,
  ammunition: (id: string) => request('/api/characters/' + id + '/inventory/ammunition') as Promise<Ammunition[]>,
  ammunitionCalibers: (id: string) => request('/api/characters/' + id + '/inventory/ammunition/calibers') as Promise<string[]>,
  createAmmunition: (id: string, body: Omit<Ammunition, 'id' | 'grenade'>) => request('/api/characters/' + id + '/inventory/ammunition', { method: 'POST', body: JSON.stringify(body) }) as Promise<Ammunition>,
  updateAmmunition: (id: string, ammunitionId: string, body: Omit<Ammunition, 'id' | 'grenade'>) => request('/api/characters/' + id + '/inventory/ammunition/' + encodeURIComponent(ammunitionId), { method: 'PUT', body: JSON.stringify(body) }) as Promise<Ammunition>,
  decrementAmmunition: (id: string, ammunitionId: string, amount: -1 | -5 | -10) => request('/api/characters/' + id + '/inventory/ammunition/' + encodeURIComponent(ammunitionId) + '/decrement', { method: 'POST', body: JSON.stringify({ amount }) }),
  consumeGrenade: (id: string, ammunitionId: string) => request('/api/characters/' + id + '/inventory/ammunition/' + encodeURIComponent(ammunitionId) + '/consume', { method: 'POST' }) as Promise<Ammunition>,
  launchGrenade: (id: string, grenadeCatalogId: string) => request('/api/characters/' + id + '/inventory/grenades/' + encodeURIComponent(grenadeCatalogId) + '/launch', { method: 'POST' }) as Promise<Ammunition>,
  grenadeCatalog: () => request('/api/grenade-catalog') as Promise<GrenadeCatalogItem[]>,
  customGrenadeCatalog: () => request('/api/grenade-catalog/custom') as Promise<GrenadeCatalogItem[]>,
  createCatalogGrenade: (body: Omit<GrenadeCatalogItem, 'id' | 'official'>) => request('/api/grenade-catalog', { method: 'POST', body: JSON.stringify(body) }) as Promise<GrenadeCatalogItem>,
  updateCatalogGrenade: (id: string, body: Omit<GrenadeCatalogItem, 'id' | 'official'>) => request('/api/grenade-catalog/' + encodeURIComponent(id), { method: 'PUT', body: JSON.stringify(body) }) as Promise<GrenadeCatalogItem>,
  deleteCatalogGrenade: (id: string) => request('/api/grenade-catalog/' + encodeURIComponent(id), { method: 'DELETE' }),
  customWeaponCatalog: () => request('/api/weapon-catalog/custom') as Promise<WeaponCatalogItem[]>,
  weaponCatalog: (slot?: string, name?: string, type?: string) => request('/api/weapon-catalog?' + new URLSearchParams(Object.entries({ slot:slot || '', name:name || '', type:type || '' }).filter(([, value]) => value) as [string,string][])) as Promise<WeaponCatalogItem[]>,
  createCatalogWeapon: (body: Omit<WeaponCatalogItem, 'id' | 'official' | 'loadedBullets'>) => request('/api/weapon-catalog', { method: 'POST', body: JSON.stringify(body) }) as Promise<WeaponCatalogItem>,
  addCatalogWeaponToCharacter: (catalogId: string, characterId: string, slot: string) => request('/api/weapon-catalog/' + encodeURIComponent(catalogId) + '/characters/' + encodeURIComponent(characterId), { method: 'POST', body: JSON.stringify({ slot }) }) as Promise<Weapon>,
  armors: (id: string) => request('/api/characters/' + id + '/inventory/armors') as Promise<Armor[]>,
  createArmorInventory: (id: string, body: unknown) => request('/api/characters/' + id + '/inventory/armors', { method: 'POST', body: JSON.stringify(body) }) as Promise<Armor>,
  updateArmor: (id: string, armorId: string, body: unknown) => request('/api/characters/' + id + '/inventory/armors/' + encodeURIComponent(armorId), { method: 'PUT', body: JSON.stringify(body) }) as Promise<Armor>,
  deleteArmor: (id: string, armorId: string) => request('/api/characters/' + id + '/inventory/armors/' + encodeURIComponent(armorId), { method: 'DELETE' }),
  armorCatalog: () => request('/api/armor-catalog') as Promise<ArmorCatalogItem[]>,
  addCatalogArmorToCharacter: (catalogId: string, characterId: string) => request('/api/armor-catalog/' + encodeURIComponent(catalogId) + '/characters/' + encodeURIComponent(characterId), { method: 'POST' }) as Promise<Armor>,
  shields: (id: string) => request('/api/characters/' + id + '/inventory/shields') as Promise<Shield[]>,
  createShieldInventory: (id: string, body: Omit<Shield, 'id'>) => request('/api/characters/' + id + '/inventory/shields', { method: 'POST', body: JSON.stringify(body) }) as Promise<Shield>,
  updateShield: (id: string, shieldId: string, body: Omit<Shield, 'id'>) => request('/api/characters/' + id + '/inventory/shields/' + encodeURIComponent(shieldId), { method: 'PUT', body: JSON.stringify(body) }) as Promise<Shield>,
  deleteShield: (id: string, shieldId: string) => request('/api/characters/' + id + '/inventory/shields/' + encodeURIComponent(shieldId), { method: 'DELETE' }),
  shieldCatalog: () => request('/api/shield-catalog') as Promise<ShieldCatalogItem[]>,
  createCatalogShield: (body: Omit<ShieldCatalogItem, 'id' | 'official'>) => request('/api/shield-catalog', { method: 'POST', body: JSON.stringify(body) }) as Promise<ShieldCatalogItem>,
  addCatalogShieldToCharacter: (catalogId: string, characterId: string) => request('/api/shield-catalog/' + encodeURIComponent(catalogId) + '/characters/' + encodeURIComponent(characterId), { method: 'POST' }) as Promise<Shield>,
  physicalShields: (id: string) => request('/api/characters/' + id + '/inventory/physical-shields') as Promise<PhysicalShield[]>,
  createPhysicalShieldInventory: (id: string, body: Omit<PhysicalShield, 'id'>) => request('/api/characters/' + id + '/inventory/physical-shields', { method: 'POST', body: JSON.stringify(body) }) as Promise<PhysicalShield>,
  updatePhysicalShield: (id: string, shieldId: string, body: Omit<PhysicalShield, 'id'>) => request('/api/characters/' + id + '/inventory/physical-shields/' + encodeURIComponent(shieldId), { method: 'PUT', body: JSON.stringify(body) }) as Promise<PhysicalShield>,
  deletePhysicalShield: (id: string, shieldId: string) => request('/api/characters/' + id + '/inventory/physical-shields/' + encodeURIComponent(shieldId), { method: 'DELETE' }),
  physicalShieldCatalog: () => request('/api/physical-shield-catalog') as Promise<PhysicalShieldCatalogItem[]>,
  addCatalogPhysicalShieldToCharacter: (catalogId: string, characterId: string) => request('/api/physical-shield-catalog/' + encodeURIComponent(catalogId) + '/characters/' + encodeURIComponent(characterId), { method: 'POST' }) as Promise<PhysicalShield>,
  reorderTraining: (id: string, activityIds: string[]) => request('/api/characters/' + id + '/training/reorder', { method: 'POST', body: JSON.stringify({ activityIds }) }),
  addTraining: (id: string, body: unknown) => request('/api/characters/' + id + '/training', { method: 'POST', body: JSON.stringify(body) }),
  updateTraining: (id: string, activityId: string, body: unknown) => request('/api/characters/' + id + '/training/' + encodeURIComponent(activityId), { method: 'PUT', body: JSON.stringify(body) }),
  deleteTraining: (id: string, activityId: string) => request('/api/characters/' + id + '/training/' + encodeURIComponent(activityId), { method: 'DELETE' }),
  list: () => request('/api/characters'),
  create: (name: string) => request('/api/characters', { method: 'POST', body: JSON.stringify({ name }) }),
  get: (id: string) => request('/api/characters/' + id),
  workspace: (id: string) => request('/api/characters/' + encodeURIComponent(id) + '/workspace') as Promise<CharacterWorkspace>,
  deleteCharacter: (id: string) => request('/api/characters/' + id, { method: 'DELETE' }),
  characterAbilities: (id: string) => request('/api/characters/' + id + '/abilities') as Promise<{ catalog: unknown[]; abilities: string[]; pendingUniqueAbilities: string[] }>,
  pendingUniqueAbilities: (id: string) => request('/api/characters/' + id + '/unique-abilities/pending'),
  decideUniqueAbility: (id: string, name: string, decision: 'accepted' | 'rejected') => request('/api/characters/' + id + '/unique-abilities/' + encodeURIComponent(name) + '/decision', { method: 'POST', body: JSON.stringify({ decision }) }),
  edit: (id: string) => request('/api/characters/' + id + '/edit', { method: 'POST' }),
  addExperience: (id: string, amount: number) => request('/api/characters/' + id + '/experience', { method: 'POST', body: JSON.stringify({ amount }) }),
  levelUp: (id: string, body: AllocationPayload) => request('/api/characters/' + id + '/level-up', { method: 'POST', body: JSON.stringify(body) }),
  levelUpAll: (id: string, body: AllocationPayload) => request('/api/characters/' + id + '/level-up-all', { method: 'POST', body: JSON.stringify(body) }),
  saveAttributeModifiers: (id: string, body: Record<string, { name: string; value: number }[]>) => request('/api/characters/' + id + '/attribute-modifiers', { method: 'PUT', body: JSON.stringify(body) }),
  save: (id: string, body: unknown) => request('/api/characters/' + id, { method: 'PUT', body: JSON.stringify(body) }),
  importLegacy: (id: string, code: string) => request('/api/characters/' + id + '/legacy/import', { method: 'POST', body: JSON.stringify({ code }) }),
  exportLegacy: async (id: string) => {
    const requestId = crypto.randomUUID();
    let response: Response;
    try { response = await fetch('/api/characters/' + id + '/legacy/export', { headers: { 'X-Request-Id': requestId }, credentials: 'same-origin' }); }
    catch (cause) { const error = new ApiError('No se ha podido conectar con el servidor.', { method: 'GET', path: safePath('/api/characters/' + id + '/legacy/export'), requestId }); logClientError(cause, { source: 'api', method: 'GET', path: error.path, requestId }); throw error; }
    if (!response.ok) { const error = await apiErrorFromResponse(response, requestId, 'GET', '/api/characters/' + id + '/legacy/export'); logClientError(error, { source: 'api', status: error.status, code: error.code, requestId: error.requestId, method: error.method, path: error.path }); throw error; }
    return response.text();
  },
  milestones: (id: string) => request('/api/characters/' + id + '/milestones'),
  cancelChanges: (id: string) => request('/api/characters/' + id + '/cancel-changes', { method: 'POST' }),
  recoverMilestone: (id: string, milestoneId: string) => request('/api/characters/' + id + '/history/' + encodeURIComponent(milestoneId) + '/recover', { method: 'POST' }),
  lastUpgrade: (id: string) => request('/api/characters/' + id + '/last-upgrade'),
  currentUpgrade: (id: string) => request('/api/characters/' + id + '/current-upgrade'),
  upgradeContext: (id: string) => request('/api/characters/' + id + '/upgrade-context'),
  abilities: () => request('/api/abilities'),
  importAbilities: (body: unknown) => request('/api/abilities/import', { method: 'POST', body: JSON.stringify(body) }),
};

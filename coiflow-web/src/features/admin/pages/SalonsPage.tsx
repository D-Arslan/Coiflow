import { useState, type FormEvent } from 'react';
import { useSalons, useCreateSalon, useUpdateSalon, useToggleSalon } from '@/features/admin/hooks/useSalons';
import { DataTable } from '@/shared/components/DataTable';
import { Modal } from '@/shared/components/Modal';
import type { Column } from '@/shared/components/DataTable';
import type { Salon, CreateSalonPayload, UpdateSalonPayload } from '@/shared/types/salon';

const columns: Column<Salon>[] = [
  { header: 'Nom', accessor: 'name' },
  { header: 'Email', accessor: 'email' },
  { header: 'Telephone', accessor: 'phone' },
  { header: 'Gerant', accessor: 'managerName' },
  {
    header: 'Statut',
    accessor: (s) => s.active ? 'Actif' : 'Inactif',
  },
];

export default function SalonsPage() {
  const { data: salons = [], isLoading } = useSalons();
  const createMutation = useCreateSalon();
  const updateMutation = useUpdateSalon();
  const toggleMutation = useToggleSalon();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingSalon, setEditingSalon] = useState<Salon | null>(null);

  const handleCreate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const payload: CreateSalonPayload = {
      name: form.get('name') as string,
      address: (form.get('address') as string) || undefined,
      phone: (form.get('phone') as string) || undefined,
      email: (form.get('email') as string) || undefined,
      managerFirstName: form.get('managerFirstName') as string,
      managerLastName: form.get('managerLastName') as string,
      managerEmail: form.get('managerEmail') as string,
      managerPassword: form.get('managerPassword') as string,
    };
    createMutation.mutate(payload, {
      onSuccess: () => setIsCreateOpen(false),
    });
  };

  const handleUpdate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingSalon) return;
    const form = new FormData(e.currentTarget);
    const payload: UpdateSalonPayload = {
      name: form.get('name') as string,
      address: (form.get('address') as string) || undefined,
      phone: (form.get('phone') as string) || undefined,
      email: (form.get('email') as string) || undefined,
    };
    updateMutation.mutate(
      { id: editingSalon.id, payload },
      { onSuccess: () => setEditingSalon(null) },
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900">Salons</h2>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          Nouveau salon
        </button>
      </div>

      <DataTable<Salon>
        columns={columns}
        data={salons}
        keyExtractor={(s) => s.id}
        isLoading={isLoading}
        emptyMessage="Aucun salon"
        actions={(salon) => (
          <div className="flex gap-2 justify-end">
            <button
              onClick={() => setEditingSalon(salon)}
              className="text-blue-600 hover:text-blue-800 text-sm font-medium"
            >
              Modifier
            </button>
            <button
              onClick={() => toggleMutation.mutate(salon.id)}
              className={`text-sm font-medium ${
                salon.active ? 'text-red-600 hover:text-red-800' : 'text-green-600 hover:text-green-800'
              }`}
            >
              {salon.active ? 'Desactiver' : 'Activer'}
            </button>
          </div>
        )}
      />

      {/* Create Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Nouveau salon">
        <form onSubmit={handleCreate} className="space-y-4">
          <fieldset className="space-y-3">
            <legend className="text-sm font-medium text-gray-700">Informations du salon</legend>
            <input name="name" required placeholder="Nom du salon *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <input name="address" placeholder="Adresse" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <div className="grid grid-cols-2 gap-3">
              <input name="phone" placeholder="Telephone" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              <input name="email" type="email" placeholder="Email" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
          </fieldset>
          <fieldset className="space-y-3">
            <legend className="text-sm font-medium text-gray-700">Compte gerant</legend>
            <div className="grid grid-cols-2 gap-3">
              <input name="managerFirstName" required placeholder="Prenom *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              <input name="managerLastName" required placeholder="Nom *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <input name="managerEmail" type="email" required placeholder="Email du gerant *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <input name="managerPassword" type="password" required minLength={8} placeholder="Mot de passe (min 8 car.) *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          </fieldset>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setIsCreateOpen(false)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
              Annuler
            </button>
            <button type="submit" disabled={createMutation.isPending} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {createMutation.isPending ? 'Creation...' : 'Creer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Modal */}
      <Modal isOpen={editingSalon !== null} onClose={() => setEditingSalon(null)} title="Modifier le salon">
        {editingSalon && (
          <form onSubmit={handleUpdate} className="space-y-4">
            <input name="name" required defaultValue={editingSalon.name} placeholder="Nom du salon *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <input name="address" defaultValue={editingSalon.address ?? ''} placeholder="Adresse" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <div className="grid grid-cols-2 gap-3">
              <input name="phone" defaultValue={editingSalon.phone ?? ''} placeholder="Telephone" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              <input name="email" type="email" defaultValue={editingSalon.email ?? ''} placeholder="Email" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setEditingSalon(null)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                Annuler
              </button>
              <button type="submit" disabled={updateMutation.isPending} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
                {updateMutation.isPending ? 'Sauvegarde...' : 'Sauvegarder'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}

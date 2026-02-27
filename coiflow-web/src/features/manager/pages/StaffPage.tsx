import { useState, type FormEvent } from 'react';
import { useStaff, useCreateStaff, useUpdateStaff, useDeleteStaff } from '@/features/manager/hooks/useStaff';
import { DataTable } from '@/shared/components/DataTable';
import { Modal } from '@/shared/components/Modal';
import type { Column } from '@/shared/components/DataTable';
import type { Staff, CreateStaffPayload, UpdateStaffPayload } from '@/shared/types/staff';

const columns: Column<Staff>[] = [
  { header: 'Nom', accessor: (s) => `${s.firstName} ${s.lastName}` },
  { header: 'Email', accessor: 'email' },
  { header: 'Commission', accessor: (s) => `${Number(s.commissionRate).toFixed(0)} %` },
];

export default function StaffPage() {
  const { data: staff = [], isLoading } = useStaff();
  const createMutation = useCreateStaff();
  const updateMutation = useUpdateStaff();
  const deleteMutation = useDeleteStaff();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState<Staff | null>(null);

  const handleCreate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const payload: CreateStaffPayload = {
      firstName: form.get('firstName') as string,
      lastName: form.get('lastName') as string,
      email: form.get('email') as string,
      password: form.get('password') as string,
      commissionRate: Number(form.get('commissionRate')),
    };
    createMutation.mutate(payload, {
      onSuccess: () => setIsCreateOpen(false),
    });
  };

  const handleUpdate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingStaff) return;
    const form = new FormData(e.currentTarget);
    const payload: UpdateStaffPayload = {
      firstName: form.get('firstName') as string,
      lastName: form.get('lastName') as string,
      commissionRate: Number(form.get('commissionRate')),
    };
    updateMutation.mutate(
      { id: editingStaff.id, payload },
      { onSuccess: () => setEditingStaff(null) },
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900">Coiffeurs</h2>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          Ajouter un coiffeur
        </button>
      </div>

      <DataTable<Staff>
        columns={columns}
        data={staff}
        keyExtractor={(s) => s.id}
        isLoading={isLoading}
        emptyMessage="Aucun coiffeur"
        actions={(s) => (
          <div className="flex gap-2 justify-end">
            <button
              onClick={() => setEditingStaff(s)}
              className="text-blue-600 hover:text-blue-800 text-sm font-medium"
            >
              Modifier
            </button>
            <button
              onClick={() => { if (confirm('Desactiver ce coiffeur ?')) deleteMutation.mutate(s.id); }}
              className="text-red-600 hover:text-red-800 text-sm font-medium"
            >
              Desactiver
            </button>
          </div>
        )}
      />

      {/* Create Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Ajouter un coiffeur">
        <form onSubmit={handleCreate} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <input name="firstName" required placeholder="Prenom *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <input name="lastName" required placeholder="Nom *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          </div>
          <input name="email" type="email" required placeholder="Email *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          <input name="password" type="password" required minLength={8} placeholder="Mot de passe (min 8 car.) *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          <input name="commissionRate" type="number" required min="0" max="100" step="0.01" placeholder="Taux de commission (%) *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setIsCreateOpen(false)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
              Annuler
            </button>
            <button type="submit" disabled={createMutation.isPending} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {createMutation.isPending ? 'Ajout...' : 'Ajouter'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Modal */}
      <Modal isOpen={editingStaff !== null} onClose={() => setEditingStaff(null)} title="Modifier le coiffeur">
        {editingStaff && (
          <form onSubmit={handleUpdate} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <input name="firstName" required defaultValue={editingStaff.firstName} placeholder="Prenom *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              <input name="lastName" required defaultValue={editingStaff.lastName} placeholder="Nom *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <p className="text-sm text-gray-500">Email : {editingStaff.email}</p>
            <input name="commissionRate" type="number" required min="0" max="100" step="0.01" defaultValue={Number(editingStaff.commissionRate)} placeholder="Taux de commission (%) *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setEditingStaff(null)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
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

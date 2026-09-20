import { create } from 'zustand';

type AppState = {
  selectedModule: string;
  setSelectedModule: (moduleName: string) => void;
};

export const useAppStore = create<AppState>((set) => ({
  selectedModule: 'Catálogo',
  setSelectedModule: (moduleName) => set({ selectedModule: moduleName }),
}));

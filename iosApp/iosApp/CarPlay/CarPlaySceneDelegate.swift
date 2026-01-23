import Foundation
import CarPlay

class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {
    
    var interfaceController: CPInterfaceController?
    var window: CPWindow? // CarPlay window for Now Playing (if needed for custom UI, usually standard is fine)
    
    // MARK: - CPTemplateApplicationSceneDelegate
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene, didConnect interfaceController: CPInterfaceController) {
        self.interfaceController = interfaceController
        print("CP: Connected to CarPlay")
        
        // create structure
        let tabBarTemplate = createTabBarTemplate()
        interfaceController.setRootTemplate(tabBarTemplate, animated: true, completion: nil)
    }
    
    func templateApplicationScene(_ templateApplicationScene: CPTemplateApplicationScene, didDisconnectInterfaceController interfaceController: CPInterfaceController) {
        self.interfaceController = nil
        print("CP: Disconnected from CarPlay")
    }
    
    // MARK: - UI Construction
    
    private func createTabBarTemplate() -> CPTabBarTemplate {
        let homeTab = createHomeTab()
        let libraryTab = createLibraryTab()
        let searchTab = createSearchTab()
        
        return CPTabBarTemplate(templates: [homeTab, libraryTab, searchTab])
    }
    
    private func createHomeTab() -> CPTemplate {
        let homeList = CPListTemplate(title: "Home", sections: [])
        homeList.tabImage = UIImage(systemName: "house.fill")
        homeList.showsTabTitle = true
        
        // Load data properly when appearing
        // Note: CPListTemplate doesn't have viewWillAppear, so we handle data loading via logic or delegate
        // For MVP, trigger load immediately
        loadHomeData(for: homeList)
        
        return homeList
    }
    
    private func createLibraryTab() -> CPTemplate {
        // Library is a navigation root with sub-items: Playlists, Albums, Artists
        let itemPlaylists = CPListItem(text: "Playlists", detailText: nil)
        itemPlaylists.setImage(UIImage(systemName: "music.note.list"))
        itemPlaylists.handler = { [weak self] item, completion in
            // Push playlists template
            self?.pushPlaylistsTemplate()
            completion()
        }
        
        let itemAlbums = CPListItem(text: "Albums", detailText: nil)
        itemAlbums.setImage(UIImage(systemName: "square.stack"))
        itemAlbums.handler = { [weak self] item, completion in
            self?.pushAlbumsTemplate()
            completion()
        }
        
        let itemArtists = CPListItem(text: "Artists", detailText: nil)
        itemArtists.setImage(UIImage(systemName: "person.2.crop.square.stack"))
        itemArtists.handler = { [weak self] item, completion in
            self?.pushArtistsTemplate()
            completion()
        }
        
        let section = CPListSection(items: [itemPlaylists, itemAlbums, itemArtists])
        let libraryList = CPListTemplate(title: "Library", sections: [section])
        libraryList.tabImage = UIImage(systemName: "books.vertical.fill")
        
        return libraryList
    }
    
    private func createSearchTab() -> CPTemplate {
        let searchTemplate = CPSearchTemplate()
        searchTemplate.tabImage = UIImage(systemName: "magnifyingglass")
        searchTemplate.searchDelegate = self
        return searchTemplate
    }
    
    // MARK: - Data Loading Helpers
    
    private func loadHomeData(for listTemplate: CPListTemplate) {
        listTemplate.updateSections([CPListSection(items: [CPListItem(text: "Loading...", detailText: nil)])])
        
        CarPlayContentManager.shared.fetchRecommendations { items in
            if items.isEmpty {
                let emptyItem = CPListItem(text: "No recommendations found", detailText: nil)
                listTemplate.updateSections([CPListSection(items: [emptyItem])])
            } else {
                // Attach handler to items
                items.forEach { $0.handler = self.itemSelectionHandler }
                let section = CPListSection(items: items, header: "Recently Played", sectionIndexTitle: nil)
                 listTemplate.updateSections([section])
            }
        }
    }
    
    // MARK: - Navigation Helpers
    
    private func pushPlaylistsTemplate() {
        let listTemplate = CPListTemplate(title: "Playlists", sections: [])
        // Show loading state
        let loadingItem = CPListItem(text: "Loading...", detailText: nil)
        listTemplate.updateSections([CPListSection(items: [loadingItem])])
        
        self.interfaceController?.pushTemplate(listTemplate, animated: true, completion: nil)
        
        CarPlayContentManager.shared.fetchPlaylists { items in
            items.forEach { $0.handler = self.itemSelectionHandler }
            listTemplate.updateSections([CPListSection(items: items)])
        }
    }
    
    private func pushAlbumsTemplate() {
        let listTemplate = CPListTemplate(title: "Albums", sections: [])
        // Show loading state
        let loadingItem = CPListItem(text: "Loading...", detailText: nil)
        listTemplate.updateSections([CPListSection(items: [loadingItem])])
        
        self.interfaceController?.pushTemplate(listTemplate, animated: true, completion: nil)
        
        CarPlayContentManager.shared.fetchAlbums { items in
            items.forEach { $0.handler = self.itemSelectionHandler }
            listTemplate.updateSections([CPListSection(items: items)])
        }
    }
    
    private func pushArtistsTemplate() {
        let listTemplate = CPListTemplate(title: "Artists", sections: [])
        // Show loading state
        let loadingItem = CPListItem(text: "Loading...", detailText: nil)
        listTemplate.updateSections([CPListSection(items: [loadingItem])])
        
        self.interfaceController?.pushTemplate(listTemplate, animated: true, completion: nil)
        
        CarPlayContentManager.shared.fetchArtists { items in
            items.forEach { $0.handler = self.itemSelectionHandler }
            listTemplate.updateSections([CPListSection(items: items)])
        }
    }
    
    // MARK: - Item Selection
    
    private var itemSelectionHandler: ((CPListItem, @escaping () -> Void) -> Void) {
        return { item, completion in
            if let mediaItem = item.userInfo as? ComposeApp.AppMediaItem {
                // Determine behavior based on item type
                // Typically: Track -> Play. Container (Album/Playlist) -> Show items or Play?
                // Logic: If it has children -> Open List. Else -> Play.
                // KMP AppMediaItem doesn't always expose children directly without fetch.
                // Simplification for "Build Now":
                // - Playlists/Albums/Artists clicked in library list -> Open detail list (Not implemented fully in fetchers yet, usually requires ID fetch)
                // - Items in Home/RecentlyPlayed -> Usually playable context -> Play
                
                // Let's implement a quick check or assumption
                // If we are in Library->Playlists->Item, it's a Playlist. We should probably open it.
                // But CarPlayContentManager.playItem just sends play command.
                
                // If it's a container (Album/Playlist), playing it usually means "Play Context".
                CarPlayContentManager.shared.playItem(mediaItem)
                
                // Show Now Playing
                self.interfaceController?.pushTemplate(CPNowPlayingTemplate.shared, animated: true, completion: nil)
            }
            completion()
        }
    }
}

// MARK: - CPSearchTemplateDelegate
extension CarPlaySceneDelegate: CPSearchTemplateDelegate {
    func searchTemplate(_ searchTemplate: CPSearchTemplate, updatedSearchText searchText: String, completionHandler: @escaping ([CPListItem]) -> Void) {
        CarPlayContentManager.shared.search(query: searchText) { items in
            // Attach play handler
            items.forEach { $0.handler = self.itemSelectionHandler }
            completionHandler(items)
        }
    }
    
    func searchTemplate(_ searchTemplate: CPSearchTemplate, selectedResult item: CPListItem, completionHandler: @escaping () -> Void) {
        // Handle selection
        self.itemSelectionHandler(item, completionHandler)
    }
}

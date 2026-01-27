import Foundation
import CarPlay
import ComposeApp

/// Manages data fetching for CarPlay using KmpHelper
class CarPlayContentManager {
    static let shared = CarPlayContentManager()
    
    private let dataSource = KmpHelper.shared.mainDataSource
    private let apiClient = KmpHelper.shared.serviceClient
    
    // MARK: - API Calls
    
    /// Fetch Recommendations (Recently Played / Home)
    func fetchRecommendations(completion: @escaping ([CPListItem]) -> Void) {
        // iOS KMP interop: access Flow via suspend function or callbacks wrapper
        // For simplicity in this plan, accessing request directly
        
        let request = Request.Library.shared.recommendations()
        
        apiClient.sendRequest(request: request) { result, error in
            guard let result = result else {
                print("CP: Error fetching recommendations: \(String(describing: error))")
                completion([])
                return
            }
            
            // Parse result to [ServerMediaItem] -> [AppMediaItem] -> [CPListItem]
            // This requires some manual bridging if not using KMP flow helpers
            
            // NOTE: In a full implementation we would use a proper KMP-Swift Flow collector
            // But for "Build Now", we use the callback approach
            
            // Assuming result can be cast/parsed.
            // Since `sendRequest` returns generic Result, we need to handle parsing.
            
            // Strategy: Use a helper on Kotlin side?
            // Or assume KMP generates ObjC generics properly?
            
            // Let's assume we get a list of objects we can map
            // Realistically, direct mapping from `Any?` in Swift is hard.
            // Better approach: Use KmpHelper to expose specific methods for Swift
            
            KmpHelper.shared.fetchRecommendations { items in
                let cpItems = items.compactMap { self.mapToCPListItem($0) }
                completion(cpItems)
            }
        }
    }
    
    func fetchPlaylists(completion: @escaping ([CPListItem]) -> Void) {
        KmpHelper.shared.fetchPlaylists { items in
            completion(items.map { self.mapToCPListItem($0) })
        }
    }
    
    func fetchAlbums(completion: @escaping ([CPListItem]) -> Void) {
        KmpHelper.shared.fetchAlbums { items in
            completion(items.map { self.mapToCPListItem($0) })
        }
    }
    
    func fetchArtists(completion: @escaping ([CPListItem]) -> Void) {
        KmpHelper.shared.fetchArtists { items in
            completion(items.map { self.mapToCPListItem($0) })
        }
    }
    
    func search(query: String, completion: @escaping ([CPListItem]) -> Void) {
        KmpHelper.shared.search(query: query) { items in
             completion(items.map { self.mapToCPListItem($0) })
        }
    }
    
    // MARK: - Action Handling
    
    func playItem(_ item: AppMediaItem) {
        // Trigger play via DataSource
        // We'll need to know which player is selected
        // For now, use the first available or last selected
        
        // This functionality needs KMP exposure too
        KmpHelper.shared.playMediaItem(item: item)
    }
    
    // MARK: - Helpers
    
    private func mapToCPListItem(_ item: AppMediaItem) -> CPListItem? {
        // Determine title, subtitle, image
        // AppMediaItem is a sealed class in Kotlin, hierarchy in Swift overrides
        
        let title = item.name
        var subtitle: String? = nil
        var imageUrl: String? = nil
        
        if let track = item as? AppMediaItem.Track {
            subtitle = track.artist?.name
            imageUrl = track.album?.image?.url
        } else if let album = item as? AppMediaItem.Album {
            subtitle = album.artist?.name
            imageUrl = album.image?.url
        } else if let artist = item as? AppMediaItem.Artist {
            imageUrl = artist.image?.url
        } else if let playlist = item as? AppMediaItem.Playlist {
            subtitle = playlist.owner
            imageUrl = playlist.image?.url
        }
        
        let listItem = CPListItem(text: title, detailText: subtitle)
        listItem.userInfo = item // Store reference for click handling
        
        // Image loading would happen asynchronously
        // CPListItem image updating is tricky after creation
        // Standard practice: Load image then call handler?
        // Or set placeholder
        
        // For MVP, we might skip remote images or use synchronous placeholder
        listItem.setImage(UIImage(systemName: "music.note"))
        
        return listItem
    }
}

#import "OOCastManagerFetcher.h"
#import <OoyalaCastSDK/OOCastManager.h>

@implementation OOCastManagerFetcher
+(OOCastManager*) fetchCastManager {
  return [OOCastManager castManagerWithAppID:@"B70158E8" namespace:@"urn:x-cast:ooyala"];
}
@end

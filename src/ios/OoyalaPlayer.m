#import <Cordova/CDVPlugin.h>
#import <UIKit/UIKit.h>

#import <OoyalaSDK/OoyalaSDK.h>
#import <OoyalaCastSDK/OOCastPlayer.h>
#import <OoyalaCastSDK/OOCastManager.h>
#import "Utils.h"
#import "OOCastManagerFetcher.h"
#import "CastPlaybackView.h"

@interface OoyalaPlayer: CDVPlugin <OOCastManagerDelegate>

@property (strong, nonatomic) OOOoyalaPlayer *ooyalaPlayer;
@property (strong, nonatomic) UIView *vPlayer;
@property (strong, nonatomic) UINavigationBar* navbar;
@property (nonatomic, strong) OOCastManager *castManager;
@property (strong, nonatomic) OOOoyalaPlayerViewController *ooyalaPlayerViewController;

@property CGSize scrSz;
@property NSUInteger navBarStart;
@property NSString *playerCloseEventCallbackID;
@property NSString *embedCode;
@property NSString *pcode;
@property NSString *playerDomain;
@property NSString *playerTitle;
@property NSString *videoImage;
@property CastPlaybackView *castPlaybackView;
@property (strong, nonatomic) CDVInvokedUrlCommand *lastCommand;

- (void) createPlayer:(CDVInvokedUrlCommand *) command;
- (void) pausePlayer:(CDVInvokedUrlCommand *) command;
- (void) resumePlayer:(CDVInvokedUrlCommand *) command;
- (void) getPlayerPlayheadTime :(CDVInvokedUrlCommand *) command;
- (void) getPlayerDuration:(CDVInvokedUrlCommand *) command;
- (void) getPlayerState:(CDVInvokedUrlCommand *) command;
- (void) getPlayerBitRate:(CDVInvokedUrlCommand *) command;
- (void) isPlayerInFullScreen:(CDVInvokedUrlCommand *) command;
- (void) isPlayerInCastMode:(CDVInvokedUrlCommand *) command;
- (void) isPlayerPlaying:(CDVInvokedUrlCommand *) command;
- (void) setPlayerPlayheadTime:(CDVInvokedUrlCommand *) command;
- (void) setPlayerSeekable:(CDVInvokedUrlCommand *) command;
- (void) setPlayerFullScreen:(CDVInvokedUrlCommand *) command;
+ (UIColor *) colorFromHexString: (NSString *) hexString;

@end


@implementation OoyalaPlayer

// used to convert hex code to ui color
+ (UIColor *)colorFromHexString:(NSString *)hexString {
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString:hexString];
    [scanner setScanLocation:1]; // bypass '#' character
    [scanner scanHexInt:&rgbValue];
    return [UIColor colorWithRed:((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha:1.0];
}

// cordova plugin code
- (void)createPlayer:(CDVInvokedUrlCommand*)command
{
    
    // discconnect from the cast service if already connected
    [self.castManager disconnectFromOoyalaPlayer];
    
    NSDictionary *dicParams = [command argumentAtIndex:0];
    NSString *pcode = (NSString *)[dicParams objectForKey:@"pcode"];
    NSString *embedCode = (NSString *)[dicParams objectForKey:@"embed_code"];
    NSString *domain = (NSString *)[dicParams objectForKey:@"domain"];
    self.playerTitle = (NSString *)[dicParams objectForKey:@"player_title"];
    self.videoImage = (NSString *)[dicParams objectForKey:@"video_image"];
    
    [self createPlayerWithPcode:pcode domain:domain embedCode:embedCode playerTitle:self.playerTitle];
    
    CDVPluginResult *pluginResult;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"[player create] success"];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) createPlayerWithPcode:(NSString *)pcode domain:(NSString *)domain embedCode:(NSString *)embedCode playerTitle:(NSString *)playerTitle
{
    NSLog(@"PCODE : %@", pcode);
    NSLog(@"DOMAIN : %@", domain);
    NSLog(@"EMBED CODE : %@", embedCode);
    
    self.pcode = pcode;
    self.playerDomain = domain;
    self.embedCode = embedCode;
    
    self.navBarStart =  17;
    
    
    // Add view for video play
    UIView *mainView = [[self viewController] view];
    
    self.scrSz = [[self viewController] view].bounds.size;
    CGFloat navigationBarHeight = 44.f;
    
    //set the device height appropriately
    if([[UIDevice currentDevice]userInterfaceIdiom]==UIUserInterfaceIdiomPhone) {
        // if its an iphone x in potrait mode, resize the screen accordingly
        if ((int)[[UIScreen mainScreen] nativeBounds].size.height == 2436){
            self.navBarStart =  27;
            self.vPlayer = [[UIView alloc] initWithFrame:CGRectMake(0,0, self.scrSz.width, (self.scrSz.height - 25.f) )];
        }
        else{
            self.vPlayer = [[UIView alloc] initWithFrame:CGRectMake(0,0, self.scrSz.width, self.scrSz.height  )];
        }
    }
    else{
          self.vPlayer = [[UIView alloc] initWithFrame:CGRectMake(0,0, self.scrSz.width, self.scrSz.height  )];
    }
    
    //set the autoresize mask
    [self.vPlayer setAutoresizingMask:UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth];
    
    //Add a navigation bar
    CGRect frame = CGRectMake(0, self.navBarStart, [[self viewController] view].frame.size.width, navigationBarHeight);
    self.navbar = [[UINavigationBar alloc] initWithFrame:frame];
    self.navbar.tag = 123456789;
    [self.navbar setAutoresizingMask:UIViewAutoresizingFlexibleWidth];
    
    UINavigationItem* navItem = [[UINavigationItem alloc] initWithTitle:playerTitle];
    [self.navbar  setTitleTextAttributes:@{NSForegroundColorAttributeName:[OoyalaPlayer colorFromHexString:@"#ffffff"]}];
    
    [self.navbar setBackgroundImage:[UIImage new] forBarMetrics:UIBarMetricsDefault];
    self.navbar.shadowImage = [UIImage new];
    self.navbar.translucent = YES;
    
    // init the cast button
    // Fetch castManager and castButton
    self.castManager = [OOCastManagerFetcher fetchCastManager];
    self.castManager.delegate = self;
    
    UIBarButtonItem *doneBtn = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:self action:@selector(onTapDone:)];
    [navItem setLeftBarButtonItem: doneBtn animated:YES];
    doneBtn.tintColor = [UIColor whiteColor];
    doneBtn.enabled = YES;
    
    UIBarButtonItem *rightButton = [[UIBarButtonItem alloc] initWithCustomView:[self.castManager castButton]];
    [navItem setRightBarButtonItem: rightButton animated:YES];
    rightButton.tintColor = [UIColor whiteColor];
    rightButton.enabled = YES;
    
    [self.navbar pushNavigationItem:navItem animated:YES];
    
    // add a touch gesture listener
    UITapGestureRecognizer *tapGesture = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(showHideNavbar:)];
    tapGesture.numberOfTapsRequired = 1;
    [mainView addGestureRecognizer:tapGesture];
    // call function to hide navbar after 3 secs
    [self hideNavBar];
    
    //set the player background color
    self.vPlayer.backgroundColor = [UIColor clearColor];
    self.vPlayer.hidden = NO;
    
    // Fetch content info and load ooyalaPlayerViewController and ooyalaPlayer
    self.ooyalaPlayer = [[OOOoyalaPlayer alloc] initWithPcode:self.pcode domain:[[OOPlayerDomain alloc] initWithString:self.playerDomain]];
    self.ooyalaPlayerViewController = [[OOOoyalaPlayerViewController alloc] initWithPlayer:self.ooyalaPlayer];
    
    [[self viewController] addChildViewController:self.ooyalaPlayerViewController];
    [self.vPlayer addSubview:self.ooyalaPlayerViewController.view];
    [self.ooyalaPlayerViewController.view setFrame: self.vPlayer.bounds];
    
    [mainView addSubview:self.navbar];
    [mainView insertSubview:self.vPlayer belowSubview:self.navbar];
    
    self.castPlaybackView = [[CastPlaybackView alloc] initWithParentView:self.vPlayer];
    [self.castManager setCastModeVideoView:self.castPlaybackView];
    
    // listen for player changes
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onCastManagerNotification:)
                                                 name:nil
                                               object:self.castManager];
    
    [[NSNotificationCenter defaultCenter] addObserver: self
                                             selector:@selector(notificationHandler:)
                                                 name:nil
                                               object:_ooyalaPlayerViewController.player];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onCastModeEnter)
                                                 name:OOCastEnterCastModeNotification
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onCastModeExit)
                                                 name:OOCastExitCastModeNotification
                                               object:nil];
    
    
    // Init the castManager in the ooyalaPlayer
    [self.ooyalaPlayer initCastManager:self.castManager];
    [self play:self.embedCode];
    
}



- (void) showHideNavbar:(UITapGestureRecognizer *)tapRecognizer
{
    // if Navigation Bar is already hidden
    if (self.navbar.hidden == YES)
    {
        // Show the Navigation Bar
        [[[[self viewController] view] viewWithTag:123456789] setHidden: NO];
        // Show the controls
        [self.ooyalaPlayerViewController showControls];
        // Then autohide it in 5 secs
        [self hideNavBar];
    }
}

-(void) hideNavBar {
    //hide it again after 5 secs
    dispatch_time_t delay = dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 5);
    dispatch_after(delay, dispatch_get_main_queue(), ^(void){
        // Hide the Navigation Bar
        [[[[self viewController] view] viewWithTag:123456789] setHidden:YES];
        // Hide the controls
        [self.ooyalaPlayerViewController hideControls];
    });
}

-(void) play:(NSString*)embedCode {
    [self.ooyalaPlayer setEmbedCode:embedCode];
    if( self.castManager.castPlayer.state != OOOoyalaPlayerStatePaused ) {
        [self.ooyalaPlayer play];
    }
}

- (IBAction)onTapDone:(id)sender
{
    
    if (!self.vPlayer.hidden) {
        [self.ooyalaPlayerViewController removeFromParentViewController];
        [self.ooyalaPlayerViewController.player pause];
        [self.ooyalaPlayerViewController.view removeFromSuperview];
        [self.vPlayer removeFromSuperview];
        [self.navbar removeFromSuperview];
        self.vPlayer = nil;
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"[playerClosed] success"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.playerCloseEventCallbackID];
        
    }
    
}

- (void) notificationHandler:(NSNotification*) notification {
    if ([notification.name isEqualToString:OOOoyalaPlayerTimeChangedNotification]) {
        [self.castPlaybackView configureCastPlaybackViewBasedOnItem:self.ooyalaPlayer.currentItem
                                                        displayName:[self getReceiverDisplayName]
                                                      displayStatus:[self getReceiverDisplayStatus]
                                                         videoTitle:[self getVideoTitle]
                                                         videoImage:[self getVideoImage]
         ];
        // return here to avoid logging TimeChangedNotificiations for shorter logs
        return;
    }
    if ([notification.name isEqualToString:OOOoyalaPlayerStateChangedNotification]) {
        [self.castPlaybackView configureCastPlaybackViewBasedOnItem:self.ooyalaPlayer.currentItem
                                                        displayName:[self getReceiverDisplayName]
                                                      displayStatus:[self getReceiverDisplayStatus]
                                                         videoTitle:[self getVideoTitle]
                                                         videoImage:[self getVideoImage]
         ];
    }
    if ([notification.name isEqualToString:OOOoyalaPlayerCurrentItemChangedNotification]) {
        [self.castPlaybackView configureCastPlaybackViewBasedOnItem:self.ooyalaPlayer.currentItem
                                                        displayName:[self getReceiverDisplayName]
                                                      displayStatus:[self getReceiverDisplayStatus]
                                                         videoTitle:[self getVideoTitle]
                                                         videoImage:[self getVideoImage]
         ];
    }
    
    NSLog(@"Notification Received: %@. state: %@. playhead: %f",
          [notification name],
          [OOOoyalaPlayer playerStateToString:[self.ooyalaPlayerViewController.player state]],
          [self.ooyalaPlayerViewController.player playheadTime]);
}
- (void)onCastModeEnter {
    [self.ooyalaPlayerViewController setFullScreenButtonShowing:NO];
    [self.ooyalaPlayerViewController setVolumeButtonShowing:YES];
}

- (void)onCastModeExit {
    [self.ooyalaPlayerViewController setVolumeButtonShowing:NO];
    [self.ooyalaPlayerViewController setFullScreenButtonShowing:YES];
}
-(void) onCastManagerNotification:(NSNotification*)notification {
    LOG( @"onCastManagerNotification: %@", notification );
}
- (UIViewController *)currentTopUIViewController {
    return [Utils currentTopUIViewController];
}
- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

-(NSString*) getReceiverDisplayName {
    NSString *name = @"Unknown";
    if( self.castManager.selectedDevice.friendlyName ) {
        name = self.castManager.selectedDevice.friendlyName;
    }
    else if( self.castManager.selectedDevice.modelName ) {
        name = self.castManager.selectedDevice.modelName;
    }
    return name;
}

-(NSString*) getReceiverDisplayStatus {
    NSString *status = @"Not connected";
    if( self.castManager.isInCastMode ) {
        switch( self.castManager.castPlayer.state ) {
            case OOOoyalaPlayerStatePlaying: { status = @"Playing"; break; }
            case OOOoyalaPlayerStatePaused: { status = @"Paused"; break; }
            case OOOoyalaPlayerStateLoading: { status = @"Buffering"; break; }
            default: { status = @"Connected"; break; }
        }
    }
    return status;
}

-(NSString*) getVideoTitle {
    NSString *title = self.playerTitle;
    return title;
}

-(NSString*) getVideoImage {
    NSString *img = self.videoImage;
    return img;
}

// Plugin functions
- (void) pausePlayer:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[pause] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        [self.ooyalaPlayerViewController.player pause];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"[pause] success"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) resumePlayer:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[resume] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        
        [self.ooyalaPlayerViewController.player play];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"[resume] success"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) getPlayerPlayheadTime :(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[getPlayerPlayheadTime ] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        int retVal = (int) self.ooyalaPlayerViewController.player.playheadTime;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:retVal];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) getPlayerDuration:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[getPlayerDuration] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        int retVal = (int) (self.ooyalaPlayerViewController.player.duration * 1000);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:retVal];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) getPlayerState:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[getPlayerState] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        OOOoyalaPlayerState state = self.ooyalaPlayerViewController.player.state;
        NSString *sState = nil;
        if (state == OOOoyalaPlayerStateInit) {
            sState = @"INIT";
        } else if (state == OOOoyalaPlayerStateLoading) {
            sState = @"LOADING";
        } else if (state == OOOoyalaPlayerStateReady) {
            sState = @"READY";
        } else if (state == OOOoyalaPlayerStatePlaying) {
            sState = @"PLAYING";
        } else if (state == OOOoyalaPlayerStatePaused) {
            sState = @"PAUSED";
        } else if (state == OOOoyalaPlayerStateCompleted) {
            sState = @"COMPLETED";
        } else if (state == OOOoyalaPlayerStateError) {
            sState = @"ERROR";
        }
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:sState];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) getPlayerBitRate:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[getPlayerBitrate] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        double retVal = self.ooyalaPlayerViewController.player.bitrate;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[NSString stringWithFormat:@"%f", retVal]];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void) isPlayerInFullScreen:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[isPlayerInFullScreen] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        BOOL retVal = [self.ooyalaPlayerViewController isFullscreen];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:(retVal ? @"true" : @"false")];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) isPlayerInCastMode:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[isPlayerInCastMode] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        BOOL retVal = [self.ooyalaPlayerViewController.player isInCastMode];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:(retVal ? @"true" : @"false")];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) isPlayerPlaying:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    
    if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[isPlayerPlaying] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        BOOL retVal = self.ooyalaPlayerViewController.player.isPlaying;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:(retVal ? @"true" : @"false")];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) isPlayerClosed:(CDVInvokedUrlCommand *) command {
    
    self.playerCloseEventCallbackID = command.callbackId;
}

- (void) setPlayerPlayheadTime:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    NSDictionary *dicParams = [command argumentAtIndex:0];
    NSString *param = (NSString *)[dicParams objectForKey:@"playHeadTime"];
    
    if (param == nil || [param isEqualToString:@"null"]) {
        errStr = @"[setPlayerPlayheadTime] failed : time is missing";
    } else if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[setPlayerPlayheadTimee] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        double time = (double)([param intValue] / 1000.0);
        [self.ooyalaPlayerViewController.player setPlayheadTime:time];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"[setPlayerPlayheadTime] success"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) setPlayerSeekable:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    NSDictionary *dicParams = [command argumentAtIndex:0];
    NSString *param = (NSString *)[dicParams objectForKey:@"playerSeekable"];
    
    if (param == nil) {
        errStr = @"[setPlayerSeekable] failed : param is missing";
    } else if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[setPlayerSeekable] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        BOOL flag = [param boolValue];
        [self.ooyalaPlayerViewController.player setSeekable:flag];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"[setPlayerSeekable] success"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) setPlayerFullScreen:(CDVInvokedUrlCommand *) command {
    NSString *errStr = nil;
    NSDictionary *dicParams = [command argumentAtIndex:0];
    NSString *param = (NSString *)[dicParams objectForKey:@"playerFullScreen"];
    
    if (param == nil) {
        errStr = @"[setPlayerFullScreen] failed : param is missing";
    } else if (self.ooyalaPlayerViewController == nil) {
        errStr = @"[setPlayerFullScreen] failed : player is not created";
    }
    
    CDVPluginResult *pluginResult;
    if (errStr) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errStr];
    } else {
        BOOL flag = [param boolValue];
        [self.ooyalaPlayerViewController setFullscreen:flag];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"[setPlayerFullScreen] success"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


@end



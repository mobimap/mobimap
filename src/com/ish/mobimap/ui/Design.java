/*****************************************************************************/
/*                    m o b i m a p :  m s r t  p r o j e c t                */
/*                         (c) 2003-2008 Ilya Shakhat                        */
/*****************************************************************************/

package com.ish.mobimap.ui;

import com.ish.mobimap.msrt;

/**
 * Design constants
 */
public class Design
{
    // scrollbar geometry
    public static final int SCROLLBAR_WIDTH = 6;

    // margins
    public static final int MARGIN_LEFT = 3;
    public static final int MARGIN_RIGHT = 3;

    // component look
    public static final int COLOR_COMPONENT_BACKGROUND = 0xffffff;
    public static final int COLOR_COMPONENT_BORDER = 0x9f9f9f;
    public static final int COLOR_COMPONENT_TEXT = 0x000000;

    // scrollbar look
    public static final int COLOR_SCROLLBAR_BACKGROUND_FROM = 0xD0CCBA;
    public static final int COLOR_SCROLLBAR_BACKGROUND_TO = 0xffffff;
    public static final int COLOR_SCROLLBAR_THUMB_FROM = 0xB2AA8B;
    public static final int COLOR_SCROLLBAR_THUMB_TO = 0xD0CCBA;
    public static final int COLOR_SCROLLBAR_BORDER = COLOR_COMPONENT_BORDER;
    public static final int COLOR_SCROLLBAR_FLARE = 0xDEDED0;

    // list look
    public static final int COLOR_LIST_BACKGROUND = 0xffffff;
    public static final int COLOR_LIST_BACKGROUND_DARK = 0xf7f7f0;
    public static final int COLOR_LIST_TEXT = 0x000060;
    public static final int COLOR_LIST_SELECTED_BACKGROUND = (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)? 0xCDC8B3: 0xEAD100;
    public static final int COLOR_LIST_SELECTED_TEXT = (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)? 0xffffff : 0x000000;
    public static final int COLOR_LIST_TITLE_BACKGROUND = 0x789B5A;
    public static final int COLOR_LIST_TITLE_TEXT = 0xffffff;

    // frame look
    public static final int COLOR_FRAME_BAR_BACKGROUND = (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)? 0x88B263: 0xB2AA8B;
    public static final int COLOR_FRAME_BAR_TEXT = 0xFFFFFF;
    public static final int COLOR_FRAME_BORDER = 0x444037;
    public static final int COLOR_FRAME_BUTTON_TEXT = 0x444037;
    public static final int COLOR_FRAME_BUTTON_TEXT_GLOW = 0xffffff;
    public static final int COLOR_FRAME_MENU_BACKGROUND = 0xffffff;
    public static final int COLOR_FRAME_MENU_TEXT = 0x444037;
    public static final int COLOR_FRAME_MENU_SELECTED_BACKGROUND = (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)? 0x88B263 : 0xB2AA8B;
    public static final int COLOR_FRAME_MENU_SELECTED_TEXT = 0xffffff;
    public static final int COLOR_FRAME_PROGRESS_BACKGROUND = 0x7fff7f;
    public static final int COLOR_FRAME_PROGRESS_TEXT = 0xffffff;

    // splash screen
    public static final int COLOR_SPLASH_BACKGROUND = 0xffffff;
    public static final int COLOR_SPLASH_PROGRESS_FROM = 0x007f00;
    public static final int COLOR_SPLASH_PROGRESS_TO = 0x7fff7f;
    public static final int COLOR_SPLASH_PROGRESS_BORDER = 0x000000;
    public static final int COLOR_SPLASH_PROGRESS_BACKGROUND = 0xffffff;
    public static final int COLOR_SPLASH_TEXT = 0x000060;

    // menu
    public static final int COLOR_MENU_TITLE_TEXT = 0xffffff;
    public static final int COLOR_MAIN_MENU_TITLE_BACKGROUND = (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)? 0x88B263: 0x789B5A;
    public static final int COLOR_HELP_MENU_TITLE_BACKGROUND = (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)? 0x06AEAD: 0x789B5A;
    public static final int COLOR_ROUTE_MENU_TITLE_BACKGROUND = (msrt.BUILD_VERSION == msrt.BUILD_MOBIMAP)? 0x505050: 0x789B5A;

    // object manager
    public static final int COLOR_OBJECT_MANAGER_TITLE_BACKGROUND = 0xB66AC0;
    public static final int COLOR_OBJECT_MANAGER_TITLE_TEXT = 0xffffff;
    public static final int COLOR_OBJECT_MANAGER_ADDRESSES_TITLE_BACKGROUND = 0x7FCFD3;
    public static final int COLOR_OBJECT_MANAGER_ADDRESSES_TITLE_TEXT = 0xffffff;

    public static final int COLOR_OBJECT_MANAGER_ONLINE_POINTS = 0xdfdfff;
    public static final int COLOR_OBJECT_MANAGER_ONLINE_MARKS = 0xefefff;


    // search engine
    public static final int COLOR_SEARCH_ENGINE_BACKGROUND_LIGHT = 0xFFFFFF;
    public static final int COLOR_SEARCH_ENGINE_BACKGROUND_DARK = 0xf0f0e8;
    public static final int COLOR_SEARCH_ENGINE_BORDER = 0xCFCFCF;
    public static final int COLOR_SEARCH_ENGINE_TEXT = 0x000000;
    public static final int COLOR_SEARCH_ENGINE_TEXT_INACTIVE = 0xCFCFCF;
    public static final int COLOR_SEARCH_ENGINE_TEXT_GREEN = 0x00CF00;
    public static final int COLOR_SEARCH_ENGINE_TEXT_BLUE = 0x0000CF;
    public static final int COLOR_SEARCH_ENGINE_TEXT_RED = 0xCF0000;
    public static final int COLOR_SEARCH_ENGINE_CURSOR = 0xA00000;

    // locator
    public static final int COLOR_LOCATOR_TITLE_BACKGROUND = 0x319123;
    public static final int COLOR_LOCATOR_TITLE_TEXT = 0xffffff;
}

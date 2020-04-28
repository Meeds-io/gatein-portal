/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import org.exoplatform.webui.form.UIFormInputIconSelector.IconSet ;
import org.exoplatform.webui.form.UIFormInputIconSelector.IconCategory;
import org.exoplatform.webui.form.UIFormInputIconSelector.CategoryIconSet ;
import org.exoplatform.webui.form.UIFormInputIconSelector.CategoryIcon ;

CategoryIcon categorySet = new CategoryIcon("misc","32x32"); 
   
  IconSet misc = 
    new IconSet("misc").
    addCategories(
        new IconCategory("Show").
        addIcon("")
    );
  
  IconSet office = 
    new IconSet("offices").
    addCategories(
        new IconCategory("Show").
        addIcon("BoxMagnifier")
    );
  
  IconSet navigation = 
    new IconSet("navigation").
    addCategories(
        new IconCategory("Show").
        addIcon("CyanUpArrowDotted")
    );
  
  IconSet tool = 
    new IconSet("tool").
    addCategories(
        new IconCategory("Show").
        addIcon("NavyWheelDataBox").addIcon("Yellowbulb")
    );
  
  IconSet user = 
    new IconSet("user").
    addCategories(
        new IconCategory("Show").
        addIcon("")
    );
  
  categorySet.addCategory(misc) ;
  categorySet.addCategory(office) ;  
  categorySet.addCategory(navigation) ;
  categorySet.addCategory(tool) ;
  categorySet.addCategory(user) ;
  
return categorySet;

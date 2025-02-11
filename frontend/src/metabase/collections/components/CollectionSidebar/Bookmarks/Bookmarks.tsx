import React from "react";
import { t } from "ttag";

import * as Urls from "metabase/lib/urls";

import Link from "metabase/collections/components/CollectionSidebar/CollectionSidebarLink";
import { LabelContainer } from "../Collections/CollectionsList/CollectionsList.styled";
import BookmarksRoot, {
  BookmarkLinkRoot,
  BookmarkTypeIcon,
} from "./Bookmarks.styled";

import { SidebarHeading } from "metabase/collections/components/CollectionSidebar/CollectionSidebar.styled";

import { BookmarkableEntities, Bookmarks } from "metabase-types/api";

interface LabelProps {
  name: string;
  type: BookmarkableEntities;
}

interface CollectionSidebarBookmarksProps {
  bookmarks: Bookmarks;
}

function getIconForEntityType(type: BookmarkableEntities) {
  const icons = {
    card: "grid",
    collection: "folder",
  };

  return icons[type];
}

const Label = ({ name, type }: LabelProps) => {
  const iconName = getIconForEntityType(type);
  return (
    <LabelContainer>
      <BookmarkTypeIcon name={iconName} />
      {name}
    </LabelContainer>
  );
};

const CollectionSidebarBookmarks = ({
  bookmarks,
}: CollectionSidebarBookmarksProps) => {
  if (bookmarks.length === 0) {
    return null;
  }

  return (
    <BookmarksRoot>
      <SidebarHeading>{t`Bookmarks`}</SidebarHeading>

      <BookmarkLinkRoot>
        {bookmarks.map(({ id, name, type }, index) => {
          const url = Urls.bookmark({ id, name, type });
          return (
            <Link key={`bookmark-${id}`} to={url}>
              <Label name={name} type={type} />
            </Link>
          );
        })}
      </BookmarkLinkRoot>
    </BookmarksRoot>
  );
};

export default CollectionSidebarBookmarks;

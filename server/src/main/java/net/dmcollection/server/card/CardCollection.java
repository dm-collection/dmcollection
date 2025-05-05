package net.dmcollection.server.card;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.dmcollection.server.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("COLLECTIONS")
public class CardCollection {
  @Id private UUID id;

  private String name;

  @CreatedDate LocalDateTime createdAt;
  @LastModifiedDate LocalDateTime updatedAt;

  @MappedCollection(idColumn = "COLLECTIONS")
  Set<CollectionCards> cards;

  @Column("USER")
  AggregateReference<User, UUID> owner;

  Boolean primary;

  @PersistenceCreator
  CardCollection(
      UUID id,
      String name,
      Set<CollectionCards> cards,
      AggregateReference<User, UUID> owner,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      Boolean primary) {
    this.id = id;
    this.name = name;
    this.cards = cards;
    this.owner = owner;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.primary = primary;
  }

  CardCollection(boolean primary) {
    this.primary = primary;
    this.cards = new HashSet<>();
  }

  CardCollection() {
    this.primary = false;
    this.cards = new HashSet<>();
  }

  public UUID getId() {
    return this.id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public void setOwner(UUID userId) {
    this.owner = AggregateReference.to(userId);
  }

  public AggregateReference<User, UUID> getOwner() {
    return this.owner;
  }

  public Set<CollectionCards> getCards() {
    return this.cards;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public boolean isPrimary() {
    return Boolean.TRUE.equals(this.primary);
  }

  public void setCardAmount(@Nonnull Long cardId, int amount) {
    removeCard(cardId);
    if (amount > 0) {
      cards.add(new CollectionCards(amount, AggregateReference.to(cardId)));
    }
  }

  public int getCardAmount(@Nonnull Long cardId) {
    return cards.stream()
        .filter(card -> Objects.equals(card.id().getId(), cardId))
        .findFirst()
        .map(CollectionCards::amount)
        .orElse(0);
  }

  private void removeCard(Long cardId) {
    cards.removeIf(card -> Objects.equals(card.id().getId(), cardId));
  }

  public void removeAllCards() {
    cards.clear();
  }
}
